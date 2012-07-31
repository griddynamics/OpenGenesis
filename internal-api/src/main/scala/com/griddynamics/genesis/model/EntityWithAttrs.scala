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
package com.griddynamics.genesis.model

import org.apache.commons.codec.binary.Base64.{encodeBase64String, decodeBase64}
import org.squeryl.annotations.Transient
import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2
import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream, ByteArrayOutputStream}

case class EntityAttr[T](name: String)

class SquerylEntityAttr(val entityId: GenesisEntity.Id,
                        val name: String,
                        val value: String)
    extends KeyedEntity[CompositeKey2[GenesisEntity.Id, String]] {

    def this() = this (0, "", "")

    override def id = CompositeKey2(entityId, name)
}

trait EntityWithAttrs extends GenesisEntity {

    @Transient private var jsonMap = Map[String, String]()
    @Transient private var objectMap = Map[String, Any]()

    @Transient private var updatedAttrs = Set[String]()
    @Transient private var removedAttrs = Set[String]()

    def apply[T](attr: EntityAttr[T])(implicit manifest: Manifest[T]): T = {
        if (removedAttrs.contains(attr.name))
            return null.asInstanceOf[T]

        objectMap.get(attr.name) match {
            case Some(value) => value.asInstanceOf[T]
            case None => jsonMap.get(attr.name) match {
                case None => null.asInstanceOf[T]
                case Some(jvalue) => {
                    val value = unmarshal[T](jvalue)
                    objectMap = objectMap + (attr.name -> value)
                    value
                }
            }
        }
    }

    def get[T](attr: EntityAttr[T])(implicit manifest: Manifest[T]): Option[T] = {
        apply(attr) match {
            case null => None
            case value => Some(value)
        }
    }

    def update[T](attr: EntityAttr[T], value: T) {
        objectMap += (attr.name -> value)

        updatedAttrs += attr.name
        removedAttrs -= attr.name
    }

    def -=[T](attr: EntityAttr[T]) {
        objectMap -= attr.name

        updatedAttrs -= attr.name
        removedAttrs += attr.name
    }

    def exportAttrs() = {
        val changedAttrs = for ((k, v) <- objectMap if updatedAttrs.contains(k))
        yield (k, marshal(v))

        (changedAttrs, removedAttrs)
    }

    def importAttrs(jsonMap: collection.Map[String, String]): this.type = {
        clearAttrs()

        this.jsonMap ++= jsonMap

        this
    }

    private def marshal[T](v : T): String = {
      val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
      val stream = new ObjectOutputStream(baos)
      stream.writeObject(v)
      encodeBase64String(baos.toByteArray)
    }

    private def unmarshal[T](bytes: String) : T = {
      val buffer: Array[Byte] = decodeBase64(bytes)
      new ObjectInputStream(new ByteArrayInputStream(buffer)).readObject().asInstanceOf[T]
    }

    def importAttrs(entity: EntityWithAttrs): this.type = {
        clearAttrs()

        jsonMap = entity.jsonMap
        objectMap = entity.objectMap

        updatedAttrs = entity.updatedAttrs
        removedAttrs = entity.removedAttrs

        this
    }

    def clearAttrs(): this.type = {
        jsonMap = Map()
        objectMap = Map()

        updatedAttrs = Set()
        removedAttrs = Set()

        this
    }
}
