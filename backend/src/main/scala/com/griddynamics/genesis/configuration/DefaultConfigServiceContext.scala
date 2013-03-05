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

package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Configuration, Bean}
import org.springframework.beans.factory.annotation._
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.apache.commons.configuration._
import com.griddynamics.genesis.service.{ConfigService, impl}
import collection.JavaConversions.{mapAsJavaMap, mapAsScalaMap}
import com.griddynamics.genesis.api.ConfigPropertyType
import com.typesafe.config._
import com.griddynamics.genesis.validation.{RegexValidator, ConfigValueValidator}
import com.griddynamics.genesis.model.ValueMetadata

@Configuration
class DefaultConfigServiceContext extends ConfigServiceContext {

  @Autowired private var dbConfig : org.apache.commons.configuration.Configuration = _
  @Autowired @Qualifier("override") private var filePropsOverride: PropertiesFactoryBean = _
  @Autowired(required = false) private var validators: java.util.Map[String, ConfigValueValidator] = mapAsJavaMap(Map())

  private val defaults = ConfigFactory.load("defaults-system").withFallback(ConfigFactory.load("genesis-plugin")).
    root.toMap.filterKeys(_.startsWith("genesis.")).mapValues {
    case co: ConfigObject => new GenesisSettingMetadata(co.toConfig)
  }
  lazy val overrideConfig = ConfigurationConverter.getConfiguration(filePropsOverride.getObject)

  private  lazy val config = {
    ConfigurationUtils.enableRuntimeExceptions(dbConfig)
    val compConfig = new CompositeConfiguration
    overrideConfig match {
      // allow comma to be used inside string property values in file
      case ac: AbstractConfiguration => ac.setDelimiterParsingDisabled(true)
      case _ =>
    }
    // read file properties overrides first
    compConfig.addConfiguration(overrideConfig)
    // then read DB, write to DB only
    compConfig.addConfiguration(dbConfig, true)
    // then read file properties defaults
    compConfig.addConfiguration(new MapConfiguration(defaults.map{
      case (k, v) => k -> v.default
    }))
    ConfigurationUtils.enableRuntimeExceptions(compConfig)
    // allow comma to be used inside string property values in DB
    compConfig.setDelimiterParsingDisabled(true)
    compConfig
  }

  @Bean def configService: ConfigService = new impl.DefaultConfigService(config, dbConfig, overrideConfig, defaults,
    validators.toMap, new RegexValidator)
}


class GenesisSettingMetadata(c: Config) extends ValueMetadata(c) {
  val propType = getStringOption("type").map(x => ConfigPropertyType.withName(x)).getOrElse(ConfigPropertyType.TEXT)
  val restartRequired = getBoolean("restartRequired")

  def isImportant = getBoolean("important")

  override def getValidation = super.getValidation +
    ("Value Length must be less than 128 characters" -> "default_length")
}

object GenesisSettingMetadata {
  def apply(default: String) = new GenesisSettingMetadata(ConfigValueFactory.fromMap(Map("default" -> default)).toConfig)
}

