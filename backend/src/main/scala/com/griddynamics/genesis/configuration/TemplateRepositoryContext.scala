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
package com.griddynamics.genesis.configuration

import com.griddynamics.genesis.service.{TemplateRepoService, Credentials}
import java.io.File
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.spring.{ApplicationContextAware, BeanClassLoaderAware}
import com.griddynamics.genesis.template._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.context.annotation.{Configuration, Bean}
import java.net.{URLClassLoader, URL}
import com.griddynamics.genesis.plugin.PluginRegistry
import com.griddynamics.genesis.service.impl.TemplateRepoServiceImpl
import com.griddynamics.genesis.api.{GenesisService, ConfigPropertyType, ConfigProperty}
import ConfigPropertyType._
import com.griddynamics.genesis.cache.CacheManager

@Configuration
class TemplateRepositoryContextImpl extends TemplateRepositoryContext
with BeanClassLoaderAware with ApplicationContextAware with Logging {

  private val NOT_SET = "NOT-SET!!!"
  private val PREFIX = "genesis.template.repository"
  private val CHARSET  = ConfigProperty(PREFIX + ".charset", "UTF-8", false, Option("Template content Character set"))
  private val WILDCARD = ConfigProperty(PREFIX + ".wildcard", "*.genesis", false, Option("Template name wildcard"))

  def charset(projectId: Int) = config.get(projectId, CHARSET.name, CHARSET.value)
  def wildcard(projectId: Int) = config.get(projectId, WILDCARD.name, WILDCARD.value)

  val commonPropDesc = Seq(CHARSET, WILDCARD)

  @Value("${genesis.template.repository.mode:classpath}") var mode: String = _

  @Value("${genesis.template.repository.pull.period.seconds:3600}") var pullPeriodSeconds: Long = _
  @Value("${genesis.template.repository.pull.on.start:true}") var pullOnStart: Boolean = _

  @Autowired var cacheManager : CacheManager = _

  @Autowired var pluginRegistry: PluginRegistry = _

  @Autowired var configContext: ConfigServiceContext = _
  lazy val config = configContext.configService

  @Autowired var storeServiceContext: StoreServiceContext = _

  @Bean def templateRepository: TemplateRepoService = {
    import collection.JavaConversions.mapAsScalaMap
    val drivers = mapAsScalaMap(applicationContext.getBeansOfType(classOf[TemplateRepositoryFactory])).values
    val plugins = pluginRegistry.getPlugins(classOf[TemplateRepositoryFactory]).values
    new TemplateRepoServiceImpl(configContext.configService, storeServiceContext.storeService,
      (drivers ++ plugins).toSeq, cacheManager)
  }

  import Modes._
  @Bean def classPathTemplateRepoFactory: TemplateRepositoryFactory = new PullingTemplateRepoFactory(new BaseTemplateRepoFactory(Classpath) {
    private val URLS = propDesc("urls", desc="Comma-separated list of Classloader URLs")

    def newTemplateRepository(implicit projectId: Int) = new ClassPathTemplateRepository(
      prop(URLS) match {
          case NOT_SET => classLoader
          case _ => new URLClassLoader(prop(URLS).split(",").map(new URL(_)).toArray)
    }, wildcard(projectId), charset(projectId))
    val settings = URLS +: commonPropDesc
  })

  @Bean def gitTemplateRepoFactory: TemplateRepositoryFactory = new PullingTemplateRepoFactory(new BaseTemplateRepoFactory(Git) {
    private val URI = propDesc("uri", desc="Git repository URI")
    private val BRANCH = propDesc("branch", desc="Git repository branch to take templates from")
    private val DIR = propDesc("directory", desc="Local directory to clone Git repository into")
    private val ID = propDesc("identity", desc="Git repository user identity")
    private val PASS = propDesc("credential", desc="Git repository user credential", propType = PASSWORD)

    private def credentials(implicit projectId: Int) = new Credentials(prop(ID), prop(PASS))

    def newTemplateRepository(implicit projectId: Int) = new GitTemplateRepository(prop(URI), credentials, prop(BRANCH),
      new File(prop(DIR)), wildcard(projectId), charset(projectId))

    val settings = Seq(URI, BRANCH, DIR, ID, PASS) ++ commonPropDesc
  })
    
  @Bean def fsRepoFactory: TemplateRepositoryFactory = new BaseTemplateRepoFactory(Local) {
    private val PATH = propDesc("path", modeStr = "fs", desc="Local Filesystem path to take templates from")

    def newTemplateRepository(implicit projectId: Int) = new FilesystemTemplateRepository(prop(PATH), wildcard(projectId))
      with SelfCachingTemplateRepository
    def settings = Seq(PATH, WILDCARD)
  }

  class PullingTemplateRepoFactory(factory: TemplateRepositoryFactory) extends TemplateRepositoryFactory {
    def newTemplateRepository(implicit projectId: Int) = factory.newTemplateRepository(projectId) match {
      case s:TemplateRepository with SelfCachingTemplateRepository => s
      case x => new PullingTemplateRepository(x, pullPeriodSeconds, pullOnStart, cacheManager)
    }

    val mode = factory.mode
    val settings = factory.settings
  }


  abstract class BaseTemplateRepoFactory(val mode: Mode) extends TemplateRepositoryFactory {
    private def propName(suffix: String, modeStr: String = mode.toString) = Seq(PREFIX, modeStr, suffix).mkString(".")
    def prop(p: ConfigProperty)(implicit projectId: Int) = config.get(projectId, p.name, p.value)
    def propDesc(suffix: String, defVal: Any = NOT_SET, desc: String = null,
                 propType: ConfigPropertyType = TEXT, modeStr: String = mode.toString) =
      ConfigProperty(propName(suffix, modeStr), String.valueOf(defVal), false, Option(desc), propType)
  }
}

