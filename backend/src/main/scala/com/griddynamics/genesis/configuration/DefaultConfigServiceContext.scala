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
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternUtils
import javax.annotation.Resource
import net.liftweb.json.{Extraction, JsonParser}
import java.io.InputStreamReader
import com.griddynamics.genesis.validation.{RegexValidator, ConfigValueValidator}

case class InputConfigProperty(default: String,
                               description: Option[String] = None,
                               `type`: Option[String] = None,
                                restartRequired: Option[Boolean] = None,
                                validation: Option[Map[String, String]] = None) {
  def propType = `type`.map(ConfigPropertyType.withName(_)).getOrElse(ConfigPropertyType.TEXT)
}

@Configuration
class DefaultConfigServiceContext extends ConfigServiceContext {

  @Autowired private var dbConfig : org.apache.commons.configuration.Configuration = _
  @Autowired @Qualifier("override") private var filePropsOverride: PropertiesFactoryBean = _
  @Resource private var rl: ResourceLoader = _
  @Autowired(required = false) private var validators: java.util.Map[String, ConfigValueValidator] = mapAsJavaMap(Map())

  private lazy val resolver = ResourcePatternUtils.getResourcePatternResolver(rl)
  private val RESOURCE_PATTERNS = Seq("classpath*:defaults-system.json", "classpath*:genesis-plugin.json")
  implicit val formats = net.liftweb.json.DefaultFormats
  private lazy val defaults = RESOURCE_PATTERNS.map(resolver.getResources(_)).flatten.map(r =>
    Extraction.extract(JsonParser.parse(new InputStreamReader(r.getInputStream)))
    (formats, manifest[Map[String,InputConfigProperty]])
  ).reduce(_ ++ _)

  lazy val overrideConfig = ConfigurationConverter.getConfiguration(filePropsOverride.getObject)

  private  lazy val config = {
    ConfigurationUtils.enableRuntimeExceptions(dbConfig)
    val compConfig = new CompositeConfiguration
    // read file properties overrides first
    compConfig.addConfiguration(overrideConfig)
    // then read DB, write to DB only
    compConfig.addConfiguration(dbConfig, true)
    // then read file properties defaults
    compConfig.addConfiguration(new MapConfiguration(defaults.map{
      case (k, v) => k -> v.default
    }))
    ConfigurationUtils.enableRuntimeExceptions(compConfig)
    compConfig
  }

  @Bean def configService: ConfigService = new impl.DefaultConfigService(config, dbConfig, overrideConfig, defaults,
  validators.toMap, new RegexValidator)
}
