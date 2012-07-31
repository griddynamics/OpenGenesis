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
package com.griddynamics.genesis.resources

import javax.servlet._
import http.{HttpServletResponse, HttpServletRequest}
import org.apache.commons.vfs._
import java.text.SimpleDateFormat
import java.util.{Calendar, Locale, Date}
import java.io.{OutputStream, InputStream}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.util.TryingUtil._
import com.griddynamics.genesis.http.CatchCodeWrapper


class ResourceFilter extends Filter with Logging {
    var resourceRoots: Seq[String] = _
    var config: FilterConfig = _
    val manager = VFS.getManager
    var cacheResources = true
    val expires: Date = {
        val cal = Calendar.getInstance()
        cal.roll(Calendar.YEAR, true)
        cal.getTime
    }

    def init(filterConfig: FilterConfig) {
        resourceRoots = filterConfig.getInitParameter(ResourceFilter.PARAM_NAME).split(";").map(
            s => s.replaceAll("classpath:", "res:")
        )
        log.debug("Resource roots: %s", resourceRoots)
        cacheResources = filterConfig.getInitParameter(ResourceFilter.CACHE_PARAMETER).toBoolean
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
        ResourceFilter.copyStream(stream, out)
        out.flush()
        out.close()
        stream.close()
    }


    def serve(response: CatchCodeWrapper, uri: String) {
        val (originalPath, cache) = if (uri == null || uri == "" || uri == "/")
            ("index.html", false)
        else
            (uri.replaceAll("^\\/", "").takeWhile(_ != ';'), true && cacheResources)

        def locateResource(path: String) = {
            resourceRoots.map ( root =>  attempt { manager.resolveFile(root + path) } ).flatten.find(_.exists)
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
        val wrappedResponse: CatchCodeWrapper = new CatchCodeWrapper(response.asInstanceOf[HttpServletResponse])
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

object ResourceFilter {
    def PARAM_NAME = "resourceRoots"
    def CACHE_PARAMETER = "cacheResources"

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
}


