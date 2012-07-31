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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.spring

import net.liftweb.json.Serialization
import org.springframework.http.converter.HttpMessageConverter
import java.nio.charset.Charset
import java.util.Collections
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.{HttpStatus, HttpOutputMessage, HttpInputMessage, MediaType}
import com.griddynamics.genesis.api.{Failure, Success, RequestResult}

class JsonMessageConverter
        extends HttpMessageConverter[AnyRef]{

    implicit val formats = net.liftweb.json.DefaultFormats
    val DEFAULT_CHARSET: Charset = Charset.forName("UTF-8")
    val supportedMediaTypes = Collections.singletonList(new MediaType("application", "json", DEFAULT_CHARSET))

    def write(t: AnyRef, contentType: MediaType, outputMessage: HttpOutputMessage) {
        val statusCode  = getStatus(t)
        if (outputMessage.isInstanceOf[ServerHttpResponse] && statusCode > 0)  {
            val response = outputMessage.asInstanceOf[ServerHttpResponse]
            response.setStatusCode(HttpStatus.valueOf(statusCode))
        }

        outputMessage.getHeaders.setContentType(MediaType.APPLICATION_JSON)
        val message: String = Serialization.write(t)
        val messageBytes: Array[Byte] = message.getBytes(DEFAULT_CHARSET.name())
        outputMessage.getHeaders.setContentLength(messageBytes.length)
        outputMessage.getBody.write(messageBytes)
    }

    private def getStatus(requestResult : AnyRef) : Int = requestResult match  {
        case RequestResult(_, _, _, _, true, _, _) => 200
        case RequestResult(_, _, _, _, false, true, _) => 404
        case RequestResult(_, _, _, _, false, _, _) => 400
        case Success(_, _) => 200
        case Failure(_, _, _, _, false, false, _) => 400
        case Failure(_, _, _, _, true, _, _) => 404
        case _ => -1
    }

    def read(clazz: Class[_ <: AnyRef], inputMessage: HttpInputMessage) = null

    def getSupportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes)

    def canWrite(clazz: Class[_], mediaType: MediaType) = true

    def canRead(clazz: Class[_], mediaType: MediaType) = true
}