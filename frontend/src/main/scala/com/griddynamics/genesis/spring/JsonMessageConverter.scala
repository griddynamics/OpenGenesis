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

import net.liftweb.json._
import org.springframework.http.converter.HttpMessageConverter
import java.nio.charset.Charset
import java.util.Collections
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.{HttpStatus, HttpOutputMessage, HttpInputMessage, MediaType}
import com.griddynamics.genesis.api.{ExtendedResult, Success, Failure}
import com.griddynamics.genesis.rest.GenesisRestController.{DEFAULT_CHARSET => DC}
import java.io.StringWriter
import com.griddynamics.genesis.rest.links.{Wrappers, ItemWrapper}

class JsonMessageConverter
        extends HttpMessageConverter[AnyRef]{

    implicit val formats = net.liftweb.json.DefaultFormats
    val DEFAULT_CHARSET: Charset = Charset.forName(DC)
    val supportedMediaTypes = Collections.singletonList(new MediaType("application", "json", DEFAULT_CHARSET))

    val ApiPackage = Package.getPackage("com.griddynamics.genesis.api")


  def write(t: AnyRef, contentType: MediaType, outputMessage: HttpOutputMessage) {
    def serializeExtendedResult(res: ExtendedResult[_]): String = {
      val json = Extraction.decompose(res) ++ JField("isSuccess", JBool(res.isSuccess))
      Printer.compact(render(json), new StringWriter()).toString
    }

    def decomposeItemWrapper(res: ItemWrapper[_]): JsonAST.JValue = {
      Extraction.decompose(res.item) ++ JField("links", Extraction.decompose(res.links))
    }

    def serializeItemWrapper(res: ItemWrapper[_]): String = {
      val json = decomposeItemWrapper(res)
      Printer.compact(render(json), new StringWriter()).toString
    }

    def decomposeCollection(wrapped: Iterable[ItemWrapper[_]]): JValue = {
      JField("items", JArray(wrapped.toList map decomposeItemWrapper))
    }

    def serializeCollectionWrapper(res: Wrappers[_]): String = {
      if (res.items.isEmpty) {
        Serialization.write(res)
      } else {
        res.items.head match {
          case wrapped: ItemWrapper[_] => {
            val json = JField("links", Extraction.decompose(res.links)) ++ decomposeCollection(res.items.asInstanceOf[Iterable[ItemWrapper[_]]])
            Printer.compact(render(json), new StringWriter()).toString
          }
          case _ => Serialization.write(res)
        }
      }
    }

    val statusCode  = getStatus(t)
    if (outputMessage.isInstanceOf[ServerHttpResponse] && statusCode > 0)  {
      val response = outputMessage.asInstanceOf[ServerHttpResponse]
      response.setStatusCode(HttpStatus.valueOf(statusCode))
    }

    outputMessage.getHeaders.setContentType(MediaType.APPLICATION_JSON)
    val message: String = t match {
      case b: ExtendedResult[_] => serializeExtendedResult(b)
      case wrapped: ItemWrapper[_] => serializeItemWrapper(wrapped)
      case collectionWrapper: Wrappers[_] => serializeCollectionWrapper(collectionWrapper)
      case _ => Serialization.write(t)
    }
    val messageBytes: Array[Byte] = message.getBytes(DEFAULT_CHARSET.name())
    outputMessage.getHeaders.setContentLength(messageBytes.length)
    outputMessage.getBody.write(messageBytes)
    }

    private def getStatus(requestResult : AnyRef) : Int = requestResult match  {
        case Success(_) => 200
        case Failure(_, _, _, _, false, _) => 400
        case Failure(_, _, _, _, true, _) => 404
        case _ => -1
    }

    def read(clazz: Class[_ <: AnyRef], inputMessage: HttpInputMessage) = { //TODO: (RB) containers(list,maps,etc) are not supported yet
      val json = parse(scala.io.Source.fromInputStream(inputMessage.getBody, DEFAULT_CHARSET.name).getLines().mkString(" "))
      clazz.cast(Extraction.extract(json, TypeInfo(clazz, None)))
    }

    def getSupportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes)

    def canWrite(clazz: Class[_], mediaType: MediaType) = true

    def canRead(clazz: Class[_], mediaType: MediaType) = true;//clazz.getPackage == ApiPackage
}