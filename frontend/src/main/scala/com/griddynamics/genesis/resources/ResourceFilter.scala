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
package com.griddynamics.genesis.resources

import javax.servlet._
import http.{HttpServletResponseWrapper, HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.util.Logging
import org.apache.commons.vfs._
import Iterator.continually
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, Date}
import org.springframework.util.FileCopyUtils
import java.io.{OutputStream, InputStream}


class ResourceFilter extends Filter with Logging {
    var resourceRoots: Seq[String] = _
    var config: FilterConfig = _
    val manager = VFS.getManager
    val expires: Date = {
        val cal = Calendar.getInstance()
        cal.roll(Calendar.YEAR, true)
        cal.getTime
    }

    def init(filterConfig: FilterConfig) {
        resourceRoots = filterConfig.getInitParameter("resourceRoots").split(",").map(
            s => s.replaceAll("classpath:", "res:")
        )
        log.debug("Resource roots: %s", resourceRoots)
        config = filterConfig
    }

    def writeContent(response: HttpServletResponse, f: FileObject, cache: Boolean) {
        response.resetBuffer()
        response.setStatus(HttpServletResponse.SC_OK)
        val content: FileContent = f.getContent
        val contentType = Option(config.getServletContext.getMimeType(f.getName.getBaseName))
        response.setHeader("Content-type", contentType.getOrElse("application/octet-stream"))
        val httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        response.setHeader("Last-modified", httpDateFormat.format(new Date(content.getLastModifiedTime)))
        if (cache) {
            response.setHeader("Expires", httpDateFormat.format(expires));
            response.setHeader("Cache-control", "public, max-age=31536000")
        } else {
            response.setHeader("Cache-control", "no-cache, no-store, must-revalidate")
        }
        val stream: InputStream = content.getInputStream
        val out: OutputStream = response.getOutputStream
        copyStream(stream, out)
        out.flush()
        out.close()
        stream.close()
    }

    def copyStream(is: InputStream, os: OutputStream) {
        val buffer = new Array[Byte](4096)
        def read() {
            val count = is.read(buffer)
            if (count != -1) {
                os.write(buffer, 0, count)
                read()
            }
        }
        read()
    }

    def serve(response: NotFoundWrapper, uri: String) {
        val (originalPath, cache) = if (uri == null || uri == "" || uri == "/")
            ("index.html", false)
        else
            (uri.replaceAll("^\\/", "").takeWhile(_ != ';'), true)
        def locateResource(path: String) = {
            resourceRoots.map(
                root => {
                    val result = try {
                        Some(manager.resolveFile(root + path))
                    }catch {
                        case e=> {
                            None
                        }
                    }
                    result
                }).filter(!_.isEmpty).map(_.get).headOption
        }
        val result: Option[FileObject] = locateResource(originalPath) match {
            case s@Some((_)) => s
            case None => {
                locateResource(originalPath + ".html")
            }
        }
        result match {
            case None => response.resume();
            case Some(f) => {
                writeContent(response, f, cache)
            }
        } 
    }



    def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req: HttpServletRequest = request.asInstanceOf[HttpServletRequest]
        val uri: String = req.getRequestURI
        val wrappedResponse: NotFoundWrapper = new NotFoundWrapper(response.asInstanceOf[HttpServletResponse])
        try {
            chain.doFilter(request, wrappedResponse)
        } finally {
            if (wrappedResponse.willServe) {
                serve(wrappedResponse, uri)
            }
        }

    }

    def destroy() {

    }
}

class NotFoundWrapper(response: HttpServletResponse) extends HttpServletResponseWrapper(response) with Logging {
    def willServe = {statusCode == HttpServletResponse.SC_NOT_FOUND}
    var statusCode : Int = _
    var message : Option[String] = None
    var callback: Unit = _
    override def setStatus(sc: Int) {
        if (checkStatus(sc, None)) {
            super.setStatus(sc)
        }
    }

    override def setStatus(sc: Int, sm: String) {
        if (checkStatus(sc, Some(sm)))
            super.setStatus(sc)
    }


    override def sendError(sc: Int, msg: String) {
        if (checkStatus(sc, Some(msg)))
            super.sendError(sc, msg)
    }

    override def sendError(sc: Int) {
        if (checkStatus(sc, None)) {
            super.sendError(sc)
        }
    }

    def resume() {
        if (! response.isCommitted) {
            message match {
                case None => getResponse.asInstanceOf[HttpServletResponse].sendError(statusCode)
                case Some(s) =>  getResponse.asInstanceOf[HttpServletResponse].sendError(statusCode, s)
            }
        }
    }

    private def checkStatus(sc: Int, msg : Option[String]) : Boolean = {
        statusCode = sc
        message = msg
        sc != HttpServletResponse.SC_NOT_FOUND
    }
}
