/**
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
package com.griddynamics.genesis.spring

import net.liftweb.json.Serialization
import org.springframework.http.converter.HttpMessageConverter
import java.nio.charset.Charset
import org.springframework.http.converter.StringHttpMessageConverter
import java.io.OutputStreamWriter
import java.util.Collections
import com.griddynamics.genesis.api.RequestResult
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.{HttpStatus, HttpOutputMessage, HttpInputMessage, MediaType}

class JsonMessageConverter
        extends HttpMessageConverter[AnyRef]{

    implicit val formats = net.liftweb.json.DefaultFormats
    val DEFAULT_CHARSET: Charset = Charset.forName("UTF-8")
    val supportedMediaTypes = Collections.singletonList(new MediaType("application", "json", DEFAULT_CHARSET))

    /*override def writeInternal(t: Object, outputMessage: HttpOutputMessage) {
        val contentType: MediaType = outputMessage.getHeaders.getContentType
        val charset: Charset = if (contentType.getCharSet != null)
                                    contentType.getCharSet
                               else StringHttpMessageConverter.DEFAULT_CHARSET

        Serialization.write(t, new OutputStreamWriter(outputMessage.getBody, charset))
    }

    override def readInternal(clazz: Class[_ <: Object], inputMessage: HttpInputMessage) = null

    override def supports(clazz: Class[_]) = true

    override def canWrite(clazz: Class[_], mediaType: MediaType) = true

    override def canRead(clazz: Class[_], mediaType: MediaType) = true

    override def getSupportedMediaTypes = Collections.singletonList(MediaType.ALL)*/


    def write(t: AnyRef, contentType: MediaType, outputMessage: HttpOutputMessage) {
        /*if(t.isInstanceOf[Option])
           t.asInstanceOf[Option] match
               case None    => ""
               case Some(v) => v*/
        val statusCode  = getStatus(t)
        if (outputMessage.isInstanceOf[ServerHttpResponse] && statusCode > 0)
            outputMessage.asInstanceOf[ServerHttpResponse].setStatusCode(HttpStatus.valueOf(statusCode))

        Serialization.write(t,
            new OutputStreamWriter(outputMessage.getBody, StringHttpMessageConverter.DEFAULT_CHARSET))
    }

    private def getStatus(requestResult : AnyRef) : Int =
        requestResult match  {
            case RequestResult(_, _, _, _, true, _) => 200
            case RequestResult(_, _, _, _, false, true) => 404
            case RequestResult(_, _, _, _, false, _) => 400
            case _ => -1
        }

    def read(clazz: Class[_ <: AnyRef], inputMessage: HttpInputMessage) = null

    def getSupportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes)

    def canWrite(clazz: Class[_], mediaType: MediaType) = true

    def canRead(clazz: Class[_], mediaType: MediaType) = true
}