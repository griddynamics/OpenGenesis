/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.json.utils

import javax.ws.rs.ext.Provider
import javax.ws.rs.{Consumes, Produces}
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import java.io.{InputStreamReader, OutputStream, Reader, InputStream}
import java.lang.reflect.{ParameterizedType, Type}
import java.lang.annotation.Annotation
import net.liftweb.json.{Serialization, JsonParser, Extraction}

@Provider
@Produces(Array("*/*"))
@Consumes(Array("*/*"))
class LiftJsonClientProvider extends AbstractMessageReaderWriterProvider[Object] {
  val untouchables = List(classOf[String], classOf[InputStream], classOf[Reader])
  val classMap = Map[AnyRef, AnyRef](classOf[collection.immutable.Seq[_]] -> classOf[List[_]])
  implicit val formats = net.liftweb.json.DefaultFormats

  def isWriteable(p1: Class[_], p2: Type, p3: Array[Annotation], p4: MediaType) = true

  def writeTo(p1: Object, p2: Class[_], p3: Type, p4: Array[Annotation], p5: MediaType, p6: MultivaluedMap[String, AnyRef], output: OutputStream) {
    p1 match {
      case obj: String => output.write(obj.getBytes)
      case _ => output.write(Serialization.write(p1).getBytes)
    }
  }

  def isReadable(klass: Class[_], genericType: Type, annotations: Array[Annotation], mediaType: MediaType) = {
    untouchables.find(k => k.isAssignableFrom(klass)).size == 0
  }

  def readFrom(klass: Class[Object], genericType: Type, annotations: Array[Annotation], mediaType: MediaType, headers: MultivaluedMap[String, String],
               entityStream: InputStream): Object = {
    import LiftJsonClientProvider._
    Extraction.extract(JsonParser.parse(new InputStreamReader(entityStream), true))(formats, manifest(genericType))
  }
}

object LiftJsonClientProvider {
  val classMap = Map[AnyRef, AnyRef](classOf[collection.immutable.Seq[_]] -> classOf[List[_]])

  def manifest(genericType: Type): Manifest[AnyRef] = {
    genericType match {
      case pt: ParameterizedType => {
        val headArg = manifest(pt.getActualTypeArguments.head)
        val tailArgs = pt.getActualTypeArguments.tail.map(manifest(_))
        val clazz = classMap.getOrElse(pt.getRawType, pt.getRawType).asInstanceOf[Class[AnyRef]]
        Manifest.classType(clazz, headArg, tailArgs: _ *)
      }
      case klass: Class[_] => Manifest.classType(klass)
      case other => throw new IllegalArgumentException("Unexpected type %s".format(other))
    }
  }
}

