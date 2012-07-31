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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.http

import javax.servlet._
import http.{HttpServletRequest, HttpServletResponse}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.net.{InetSocketAddress, HttpURLConnection, URL}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import com.griddynamics.genesis.util.Logging
import java.security.Principal
import org.springframework.security.core.context.{SecurityContext, SecurityContextHolder}
import com.griddynamics.genesis.resources.ResourceFilter
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import java.util.concurrent.{CountDownLatch, TimeUnit, Executors}
import java.util.zip.GZIPInputStream

sealed trait Tunnel {
    def backendHost: String
    def uriMatch: String
    def doServe(request: HttpServletRequest, response: CatchCodeWrapper)
    def connectTimeout: Int
    def readTimeout: Int
}

abstract class TunnelFilter(override val uriMatch: String) extends Filter with Tunnel with Logging {
    var backendHost: String = _
    var connectTimeout: Int = 5000
    var readTimeout: Int = 5000


    def init(filterConfig: FilterConfig) {
        backendHost = filterConfig.getInitParameter(TunnelFilter.BACKEND_PARAMETER)
        connectTimeout = filterConfig.getInitParameter(TunnelFilter.CONNECT_TIMEOUT).toInt
        readTimeout = filterConfig.getInitParameter(TunnelFilter.READ_TIMEOUT).toInt
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
          TunnelFilter.withTimings(doServe(request, response))
          response.getOutputStream.flush()
        } else {
            response.resume()
        }
    }

    def destroy() {}
}

object TunnelFilter extends Logging {
    val BACKEND_PARAMETER = "backendHost"
    val READ_TIMEOUT = "readTimeout"
    val CONNECT_TIMEOUT = "connectTiemout"
    val SEC_HEADER_NAME = "X-On-Behalf-of"
    val AUTH_HEADER_NAME = "X-Authorities"
    val TUNNELED_HEADER_NAME = "X-Tunneled-By"
    val SEPARATOR_CHAR = ","
    def currentUser = {
        val context: SecurityContext = SecurityContextHolder.getContext
        val result = if (context != null && context.getAuthentication != null)
         context.getAuthentication.asInstanceOf[Principal].getName
        else
         ""
        result
    }

    def withTimings[B](block: => B) : B = {
      val millis = System.currentTimeMillis()
      try {
        block
      } finally {
        log.trace("Time spent: %s ms", System.currentTimeMillis() - millis)
      }
    }

    def authorities = {
        val context: SecurityContext = SecurityContextHolder.getContext
        if (context != null && context.getAuthentication != null)
         context.getAuthentication.getAuthorities
        else
         java.util.Collections.emptyList()
    }
}


trait NettyTunnel extends Tunnel with Logging {
    val bossPool = Executors.newFixedThreadPool(5)
    val handlerPool = Executors.newFixedThreadPool(5 * 3)
    var finished = false

    def doServe(request: HttpServletRequest, response: CatchCodeWrapper) {
        val url = new URL(backendHost + "/" + request.getRequestURI)
        val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(bossPool, handlerPool))
        val latch = new CountDownLatch(1) //we have to block until remote response will be read
        val handler = new OutputStreamHandler(response, latch)
        var factory: TunnelPipelineFactory = new TunnelPipelineFactory(handler)
        bootstrap.setPipelineFactory(factory)
        bootstrap.setOption("connectTimeoutMillis", connectTimeout)
        val future = bootstrap.connect(new InetSocketAddress(url.getHost, url.getPort))
        val channel = future.awaitUninterruptibly().getChannel
        factory.getPipeline.getChannel
        if (!future.isSuccess) {
            response.resetBuffer()
            response.sendError(502, "Cannot connect to backend")
        } else {
            val write: ChannelFuture = channel.write(prepareRequest(request, url))
            write.awaitUninterruptibly(readTimeout / 1000, TimeUnit.SECONDS)
            channel.getCloseFuture.awaitUninterruptibly(readTimeout / 1000, TimeUnit.SECONDS)
        }
        if (! latch.await(readTimeout / 1000, TimeUnit.SECONDS)) {
          response.resetBuffer()
          response.sendError(502, "Timeout while reading remote answer")
        }
    }

    def prepareRequest(req: HttpServletRequest, host: URL): HttpRequest = {
        val uri = Option(req.getQueryString) match {
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
        pipeline.addLast("handler", handler)
        pipeline
    }
}

class OutputStreamHandler(val response: HttpServletResponse, val latch: CountDownLatch)
  extends SimpleChannelUpstreamHandler with Logging {

    var chunked: Boolean = false

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        log.error(e.getCause, "Exception during working with remote end: %s", e)
        if (! response.isCommitted) {
            response.resetBuffer()
            response.sendError(503, "Remote party error")
        }
        latch.countDown()
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        if (! chunked) {
            val remote = e.getMessage.asInstanceOf[DefaultHttpResponse]
            response.reset()
            response.setStatus(remote.getStatus.getCode)
            response.addHeader(TunnelFilter.TUNNELED_HEADER_NAME, "Netty %s".format(this))
            import scala.collection.JavaConversions._
            val headers = remote.getHeaderNames
            for (header <- headers if (header != "Connection" && header != "Transfer-Encoding" && header != "Set-Cookie")) {
                for (vals <- remote.getHeaders(header)) {
                    response.addHeader(header, vals)
                }
            }
            if (e.getMessage.asInstanceOf[HttpMessage].isChunked) {
                chunked = true
                log.debug("Starting to read chunks")
            } else {
                write(remote.getContent)
                latch.countDown()
            }
        } else {
            val chunk = e.getMessage.asInstanceOf[HttpChunk]
            log.debug("Reading chunk")
            write(chunk.getContent)
            if (chunk.isLast) {
                log.debug("Last chunk")
                chunked = false
                response.getOutputStream.flush()
                latch.countDown()
            }
        }
        def write(content: ChannelBuffer) {
            if (content.readable() && ! response.isCommitted) {
               val bytes = content.readableBytes()
               response.getOutputStream.write(content.array().slice(0, bytes))
            }
        }
    }
}

trait UrlConnectionTunnel extends Tunnel with Logging {
    import collection.JavaConversions.collectionAsScalaIterable
    def doServe(request: HttpServletRequest, response: CatchCodeWrapper) {
        val uri = Option(request.getQueryString) match {
            case Some(s) => request.getRequestURI + "?" + s
            case _ => request.getRequestURI
        }
        val url = new URL(backendHost + uri)
        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestMethod(request.getMethod)
        connection.setUseCaches(false)
        var doWrite = false
        if (request.getMethod == "POST" || request.getMethod == "PUT") {
            connection.setDoOutput(true)
            doWrite = true
        }
        connection.setDoInput(true)
        connection.addRequestProperty(TunnelFilter.SEC_HEADER_NAME, TunnelFilter.currentUser)
        connection.addRequestProperty(TunnelFilter.AUTH_HEADER_NAME, TunnelFilter.authorities.mkString(TunnelFilter.SEPARATOR_CHAR))
        connection.addRequestProperty("Connection", "close") //no keep-alive
        connection.setConnectTimeout(connectTimeout)
        connection.setReadTimeout(readTimeout)
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
            //it's to avoid double encoding
            response.setStatus(connection.getResponseCode)
            val stream = try {
              connection.getInputStream
            } catch {
              //on codes >= 400 connection.getInputStream throws error, but all data are in connection.errorStream
              case e => connection.getErrorStream
            }
            import scala.collection.JavaConversions._
            //I have no idea why there is nulls for header names, but we have to check it
            for(entry <- connection.getHeaderFields if (entry._1 != null && entry._1 != "Connection"
              && entry._1 != "Set-Cookie")) {
                response.addHeader(entry._1, entry._2(0))
            }
            response.addHeader(TunnelFilter.TUNNELED_HEADER_NAME, "UrlConnection")
            response.setHeader("Content-Encoding", "identity")
            log.trace("Estimate byte count %d", stream.available())
            val streamToRead = if ("gzip" == connection.getContentEncoding)
                new GZIPInputStream(stream)
            else
                stream
            ResourceFilter.copyStream(streamToRead, localOut)
            streamToRead.close()
            try {
                stream.close()
            } catch {
                case e => log.debug("Error when re-closing stream")
            }
            localOut.flush()
        } catch {
            case e => {
                log.error(e, "Error when tunneling request")
                response.resetBuffer()
                response.sendError(503, "Error reading remote answer")
            }
        } finally {
            connection.disconnect()
        }
    }
}




