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

package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.api
import api.ConfigPropertyType
import com.griddynamics.genesis.api.ConfigPropertyType.ConfigPropertyType
import collection.JavaConversions.asScalaIterator
import org.springframework.transaction.annotation.Transactional
import org.apache.commons.configuration.Configuration


// TODO: add synchronization?
class DefaultConfigService(val config: Configuration, val writeConfig: Configuration, val configRO: Configuration,
                           val descriptions: Map[String, String] = Map(),
                           val propertyTypes: Map[String, ConfigPropertyType] = Map()) extends service.ConfigService {

  import service.GenesisSystemProperties._
    @Transactional(readOnly = true)
    def get[B](name: String, default: B): B = {
      (default match {
        case value: Int => config.getInt(name, value)
        case value: Long => config.getLong(name, value)
        case value: String => config.getString(name, value)
        case value: Boolean => config.getBoolean(name, value)
        case _ => throw new IllegalArgumentException("Not supported type")
      }).asInstanceOf[B]
    }

    @Transactional(readOnly = true)
    def get[B](projectId: Int, name: String, default: B) = {
      val projPropName = mkProjectPrefix(projectId, name)
      get(projPropName).asInstanceOf[Option[B]] getOrElse get(name, default)
    }

    @Transactional(readOnly = true)
    def get(name: String) = Option(config.getProperty(name))

    private def isReadOnly(key: String) = key.startsWith(PREFIX_DB) || configRO.containsKey(key)
    
    private def desc(key: String) = descriptions.get(key)

    private def propertyType(key: String) = propertyTypes.getOrElse(key, ConfigPropertyType.TEXT)

    @Transactional(readOnly = true)
    def listSettings(prefix: Option[String]) = prefix.map(config.getKeys(_)).getOrElse(config.getKeys())
         .map(k => api.ConfigProperty(k, config.getString(k), isReadOnly(k), desc(k), propertyType(k))).toSeq.sortBy(_.name)

  private def mkProjectPrefix(projectId: Int, prefix:String) = Seq(PROJECT_PREFIX, projectId, prefix.stripPrefix(PREFIX_GENESIS)).filter("" != _).mkString(".")

    @Transactional
    def update(configuration: Map[String, Any]) = configuration.foreach {
        case (name, _) if isReadOnly(name) => throw new IllegalArgumentException("Could not modify read-only property: " + name)
        case (name, value) => writeConfig.setProperty(name, value)
    }

    @Transactional
    def delete(key: String) = isReadOnly(key) match {
        case true => throw new IllegalArgumentException("Could not modify read-only property")
        case _ => writeConfig.clearProperty(key)
    }

    @Transactional
    def clear(prefix: Option[String]) {prefix.map(writeConfig.subset(_)).getOrElse(writeConfig).clear}

  def update(projectId: Int, config: Map[String, Any]) {
    update(config.map{case (name, value) => mkProjectPrefix(projectId, name) -> value})
  }
}
