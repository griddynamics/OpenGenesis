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
package com.griddynamics.genesis.client

import java.lang.annotation.Annotation
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import com.griddynamics.genesis.api.GenesisService
import java.io.{InputStreamReader, InputStream}
import net.liftweb.json._
import javax.ws.rs.ext.{Provider, MessageBodyReader}
import java.lang.reflect.{ParameterizedType, Type}
import javax.ws.rs.Consumes
import reflect.Manifest
import java.lang.RuntimeException

/**
 * @author Victor Galkin
 */

@Provider
@Consumes
class ClientMessageBodyReader extends MessageBodyReader[AnyRef] {

    import ClientMessageBodyReader._

    def readFrom(clazz: Class[AnyRef], genericType: Type, annotations: Array[Annotation],
                 mediaType: MediaType, httpHeaders: MultivaluedMap[String, String], entityStream: InputStream) = {
        val tmanifest = manifest(genericType)
        Extraction.extract(JsonParser.parse(new InputStreamReader(entityStream), false))(formats, tmanifest): AnyRef
    }


    def isReadable(clazz: Class[_], genericType: Type, annotations: Array[Annotation], mediaType: MediaType) =
        clazz.getPackage.getName.startsWith(classOf[GenesisService].getPackage.getName) ||
            clazz.getPackage.getName.startsWith("scala.collection")
}

object ClientMessageBodyReader {
    private implicit val formats: Formats = DefaultFormats

    val classMap = Map[AnyRef, AnyRef](classOf[collection.immutable.Seq[_]] -> classOf[List[_]])

    def manifest(genericType: Type): Manifest[AnyRef] = {
        genericType match {
            case pt: ParameterizedType => {
                val headArg = manifest(pt.getActualTypeArguments.head)
                val tailArgs = pt.getActualTypeArguments.tail.map(manifest(_))

                val clazz = classMap.getOrElse(pt.getRawType, pt.getRawType).asInstanceOf[Class[AnyRef]]

                Manifest.classType(clazz, headArg, tailArgs: _ *)
            }
            case cl: Class[_] => Manifest.classType(cl)
            case t => throw new RuntimeException("Unexpected type %s".format(t))
        }
    }
}
