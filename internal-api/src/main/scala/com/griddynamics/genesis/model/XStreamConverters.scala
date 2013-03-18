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

package com.griddynamics.genesis.model

import com.thoughtworks.xstream.mapper.Mapper
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import com.thoughtworks.xstream.converters.{UnmarshallingContext, MarshallingContext}
import collection.mutable.ListBuffer


class XStreamListConverter(mapper: Mapper) extends AbstractCollectionConverter(mapper) {

  def canConvert( clazz: Class[_]) = {
    classOf[::[_]] == clazz
  }

  def marshal( value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) =
    value match {
      case seq: Seq[_] => for ( item <- seq ) {
        writeItem(item, context, writer)
      }
    }


  def unmarshal( reader: HierarchicalStreamReader, context: UnmarshallingContext ) = {
    val list = new ListBuffer[Any]()
    while (reader.hasMoreChildren) {
      reader.moveDown
      val item = readItem(reader, context, list)
      list += item
      reader.moveUp
    }
    list.toSeq
  }
}

class XStreamSomeConverter(mapper: Mapper) extends AbstractCollectionConverter(mapper) {

  def canConvert( clazz: Class[_]) = {
    classOf[Some[_]] == clazz
  }

  def marshal( value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) =
    value match {
      case Some(item) => writeItem(item, context, writer)
    }


  def unmarshal( reader: HierarchicalStreamReader, context: UnmarshallingContext ) = {
    val list = new ListBuffer[Any]()
    while (reader.hasMoreChildren) {
      reader.moveDown
      val item = readItem(reader, context, list)
      list += item
      reader.moveUp
    }
    list.headOption
  }
}
