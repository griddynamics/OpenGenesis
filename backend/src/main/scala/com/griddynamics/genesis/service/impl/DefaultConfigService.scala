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
import api.{ExtendedResult, Success}
import collection.JavaConversions.asScalaIterator
import org.springframework.transaction.annotation.Transactional
import org.apache.commons.configuration.Configuration
import com.griddynamics.genesis.configuration.DefaultSetting
import com.griddynamics.genesis.validation.ConfigValueValidator


// TODO: add synchronization?
class DefaultConfigService(val config: Configuration, val writeConfig: Configuration, val configRO: Configuration,
                           val defaults: Map[String, DefaultSetting] = Map(),
                           validators: Map[String, ConfigValueValidator],
                           defaultValidator: ConfigValueValidator) extends service.ConfigService {

  import service.GenesisSystemProperties._

  lazy val initialConfig = listSettings(None).filter(_.restartRequired).map(cp => (cp.name, config.getProperty(cp.name))).toMap

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

    @Transactional(readOnly = true)
    def listSettings(prefix: Option[String]) = prefix.map(config.getKeys(_)).getOrElse(config.getKeys()).map(k => {
      val default = defaults.getOrElse(k, DefaultSetting("NOT-SET!!!"))
      api.ConfigProperty(k, config.getString(k), isReadOnly(k), default.description, default.propType, default.restartRequired)
    }).toSeq.sortBy(_.name)

  private def mkProjectPrefix(projectId: Int, prefix:String) = Seq(PROJECT_PREFIX, projectId, prefix.stripPrefix(PREFIX_GENESIS)).filter("" != _).mkString(".")

  @Transactional
  def update(configuration: Map[String, Any]) = configuration.map {
    case (name, _) if isReadOnly(name) => throw new IllegalArgumentException("Could not modify read-only property: " + name)
    case (name, value) => validate(name, value.toString) match {
      case s: Success[_] => writeConfig.setProperty(name, value)
      s
      case f => f
    }
  }.reduceOption(_ ++ _).getOrElse(Success(None))

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

  def restartRequired() = initialConfig.exists {
    case (k, v) => v != config.getProperty(k)
  }

  private def validate(propName: String, value: String): ExtendedResult[Any] =
    (for {prop <- defaults.get(propName).toSeq
          (msg, validatorName) <- prop.getValidation
          validator = validators.getOrElse(validatorName, defaultValidator)} yield
      validator.validate(propName, value, msg, Map("name" -> validatorName))
    ).reduceOption(_ ++ _).getOrElse(Success(value))

  def validateSettings = config.getKeys.map(k => validate(k, config.getString(k))).reduceOption(_ ++ _)
    .getOrElse(Success(None))

  def isImportant(name: String) = defaults.get(name).map(_.isImportant).getOrElse(false)
}
