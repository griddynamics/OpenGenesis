/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */

package com.griddynamics.genesis.http

import javax.servlet._
import http.{HttpServletRequest, HttpServletResponse}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.net.{InetSocketAddress, HttpURLConnection, URL}
import org.jboss.netty.channel._
import local.DefaultLocalClientChannelFactory
import org.jboss.netty.handler.codec.http._
import com.griddynamics.genesis.util.Logging
import java.security.Principal
import org.springframework.security.core.context.{SecurityContext, SecurityContextHolder}
import com.griddynamics.genesis.resources.ResourceFilter
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import collection.mutable.{ArrayOps, ArrayBuffer}
import java.io.{IOException, IOError, OutputStream}
import socket.ClientSocketChannelFactory
import socket.http.HttpTunnelingClientSocketChannelFactory
import socket.oio.OioClientSocketChannelFactory
import java.util.concurrent.{ThreadPoolExecutor, TimeUnit, Executors}

sealed trait Tunnel {
    def backendHost: String
    def uriMatch: String
    def doServe(request: HttpServletRequest, response: CatchCodeWrapper)
}

abstract class TunnelFilter(override val uriMatch: String) extends Filter with Tunnel with Logging {
    var backendHost: String = _

    def init(filterConfig: FilterConfig) {
        backendHost = filterConfig.getInitParameter(TunnelFilter.BACKEND_PARAMETER)
    }

    def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val rq = request.asInstanceOf[HttpServletRequest]
        val wrapped = new CatchCodeWrapper(response.asInstanceOf[HttpServletResponse], Array(404, 405))
        try {
            chain.doFilter(request, wrapped)
        } finally {
            if (wrapped.willServe)
                serve(rq, wrapped)
        }
    }

    def serve(request: HttpServletRequest, response: CatchCodeWrapper) {
        if (request.getRequestURI.startsWith(uriMatch)) {
            doServe(request, response)
            response.getOutputStream.flush()
        } else {
            response.resume()
        }
    }

    def destroy() {}
}

object TunnelFilter {
    val BACKEND_PARAMETER = "backendHost"
    val SEC_HEADER_NAME = "X-On-Behalf-of"
    val TUNNELED_HEADER_NAME = "X-Tunneled-By"
    def currentUser = {
        var context: SecurityContext = SecurityContextHolder.getContext
        val result = if (context != null && context.getAuthentication != null)
         context.getAuthentication.asInstanceOf[Principal].getName
        else
         ""
        result
    }
}


trait NettyTunnel extends Tunnel with Logging {
    val bossPool = Executors.newFixedThreadPool(5)
    val handlerPool = Executors.newFixedThreadPool(5 * 3)
    val workerPool = Executors.newFixedThreadPool(5)
    var finished = false

    def doServe(request: HttpServletRequest, response: CatchCodeWrapper) {
        val url = new URL(backendHost + "/" + request.getRequestURI)
        val bootstrap = new ClientBootstrap(new OioClientSocketChannelFactory(bossPool))
        val handler = new OutputStreamHandler(response, this)
        var factory: TunnelPipelineFactory = new TunnelPipelineFactory(handler)
        bootstrap.setPipelineFactory(factory)
        bootstrap.setOption("connectTimeoutMillis", 5000)
        val future = bootstrap.connect(new InetSocketAddress(url.getHost, url.getPort))
        val channel = future.awaitUninterruptibly().getChannel
        factory.getPipeline.getChannel
        if (!future.isSuccess) {
            response.resetBuffer()
            response.sendError(502, "Cannot connect to backend")
        } else {
            var write: ChannelFuture = channel.write(prepareRequest(request, url))
            write.awaitUninterruptibly(5, TimeUnit.SECONDS)
            log.debug("Channel is %s", channel)
            channel.getCloseFuture.awaitUninterruptibly(5, TimeUnit.SECONDS)
        }
        log.debug("Exiting")
    }

    def prepareRequest(req: HttpServletRequest, host: URL): HttpRequest = {
        var uri = Option(req.getQueryString) match {
            case Some(s) => req.getRequestURI + "?" + s
            case _ => req.getRequestURI
        }
        val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        request.setHeader(HttpHeaders.Names.HOST, host.getHost);
        request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        if (req.getContentType != null)
            request.setHeader(HttpHeaders.Names.CONTENT_TYPE, req.getContentType);
        request.setHeader(TunnelFilter.SEC_HEADER_NAME, TunnelFilter.currentUser)
        request.setMethod(HttpMethod.valueOf(req.getMethod))
        val input = Iterator.continually(req.getInputStream.read).takeWhile(-1 != _).map(_.toByte).toArray
        val buffer = ChannelBuffers.copiedBuffer(input)
        request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes())
        request.setContent(buffer)
        request
    }
}


class TunnelPipelineFactory(handler: OutputStreamHandler) extends ChannelPipelineFactory {
    def getPipeline = {
        val pipeline: ChannelPipeline = new DefaultChannelPipeline()
        pipeline.addLast("codec", new HttpClientCodec())
        pipeline.addLast("inflate", new HttpContentDecompressor)
        pipeline.addLast("aggregator", new HttpChunkAggregator(64000))
        pipeline.addLast("handler", handler)
        pipeline
    }
}

class OutputStreamHandler(val response: HttpServletResponse, tunnel: NettyTunnel)
  extends SimpleChannelUpstreamHandler with Logging {

    var chunked: Boolean = false

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        log.error(e.getCause, "Exception during working with remote end: %s", e)
        if (! response.isCommitted) {
            response.resetBuffer()
            response.sendError(503, "Remote party error")
        }
        tunnel.finished = true
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        log.debug("Input channel: %s", e.getChannel)
        if (! chunked) {
            log.debug("Here")
            val remote = e.getMessage.asInstanceOf[DefaultHttpResponse]
            response.reset()
            response.setStatus(remote.getStatus.getCode)
            response.addHeader(TunnelFilter.TUNNELED_HEADER_NAME, "Netty %s".format(this))
            import scala.collection.JavaConversions._
            val headers = remote.getHeaderNames
            for (header <- headers if (header != "Connection" && header != "Transfer-Encoding")) {
                for (vals <- remote.getHeaders(header)) {
                    response.addHeader(header, vals)
                }
            }
            if (e.getMessage.asInstanceOf[HttpMessage].isChunked) {
                chunked = true
                log.debug("Starting to read chunks")
            } else {
                response.getOutputStream.write(remote.getContent.toByteBuffer.array())
                tunnel.finished = true
            }
        } else {
            val chunk = e.getMessage.asInstanceOf[HttpChunk]
            log.debug("Reading chunk %s", chunk)
            if (chunk.isLast) {
                log.debug("Last chunk")
                chunked = false
                tunnel.finished = false
            }
        }
    }

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        log.debug("Channel was closed")
    }
}

trait UrlConnectionTunnel extends Tunnel with Logging {
    def doServe(request: HttpServletRequest, response: CatchCodeWrapper) {
        val uri = Option(request.getQueryString) match {
            case Some(s) => request.getRequestURI + "?" + s
            case _ => request.getRequestURI
        }
        val url = new URL(backendHost + uri)
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestMethod(request.getMethod)
        var doWrite = false
        if (request.getMethod == "POST" || request.getMethod == "PUT") {
            connection.setDoOutput(true)
            doWrite = true
        }
        connection.setDoInput(true)
        connection.addRequestProperty(TunnelFilter.SEC_HEADER_NAME, TunnelFilter.currentUser)
        connection.addRequestProperty("Connection", "close") //no keep-alive
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)
        try {
            connection.connect()
            if (doWrite) {
                val remoteOut = connection.getOutputStream
                ResourceFilter.copyStream(request.getInputStream, remoteOut)
                remoteOut.flush()
                remoteOut.close()
            }
            val localOut = response.getOutputStream
            response.resetBuffer()
            val code = try {
                connection.getResponseCode
            } catch {
                case e: IOException => {
                    log.debug("error occured")
                    connection.getResponseCode
                }
            }
            val stream = try {
                connection.getInputStream
            } catch {
                case e => {
                    connection.getErrorStream
                }
            }
            response.setStatus(code)
            import scala.collection.JavaConversions._
            for(entry <- connection.getHeaderFields if (entry._1 != null && entry._1 != "Connection")) {
                response.addHeader(entry._1, entry._2(0))
            }
            response.addHeader(TunnelFilter.TUNNELED_HEADER_NAME, "UrlConnection")
            ResourceFilter.copyStream(stream, localOut)
            stream.close()
            localOut.flush()
            localOut.close()
        } catch {
            case e => {
                log.error(e, "Error when tunneling request")
                response.resume()
            }
        } finally {
            connection.disconnect()
        }
    }
}




