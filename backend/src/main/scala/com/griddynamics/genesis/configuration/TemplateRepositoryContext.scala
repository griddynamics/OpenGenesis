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

import com.griddynamics.genesis.service.Credentials
import java.io.File
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.spring.{ApplicationContextAware, BeanClassLoaderAware}
import com.griddynamics.genesis.template._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import net.sf.ehcache.CacheManager
import org.springframework.context.annotation.{Configuration, Bean}
import java.net.{URLClassLoader, URL}
import com.griddynamics.genesis.plugin.PluginRegistry
import com.griddynamics.genesis.service.impl.TemplateRepoServiceImpl

@Configuration
class TemplateRepositoryContextImpl extends TemplateRepositoryContext
                                       with BeanClassLoaderAware with ApplicationContextAware with Logging {

    def charset(projectId: Int) = config.get(projectId, "genesis.template.repository.charset", "UTF-8")
    def wildcard(projectId: Int) = config.get(projectId, "genesis.template.repository.wildcard", "*.genesis")

    @Value("${genesis.template.repository.mode:classpath}") var mode: String = _

    @Value("${genesis.template.repository.pull.period.seconds:3600}") var pullPeriodSeconds: Long = _
    @Value("${genesis.template.repository.pull.on.start:true}") var pullOnStart: Boolean = _

    @Autowired var cacheManager : CacheManager = _

    @Autowired var pluginRegistry: PluginRegistry = _

    @Autowired var configContext: ConfigServiceContext = _
    lazy val config = configContext.configService
  private val NOT_SET = "NOT-SET!!!"
  private val PREFIX = "genesis.template.repository"
    @Bean
    def templateRepository = {
        import collection.JavaConversions.mapAsScalaMap
        val drivers = mapAsScalaMap(applicationContext.getBeansOfType(classOf[TemplateRepositoryFactory])).values
        val plugins = pluginRegistry.getPlugins(classOf[TemplateRepositoryFactory]).values
        new TemplateRepoServiceImpl(configContext.configService, (drivers ++ plugins).toSeq, cacheManager)
    }

  import Modes._
    @Bean def classPathTemplateRepoFactory = new PullingTemplateRepoFactory(new BaseTemplateRepoFactory(Classpath) {
      def newTemplateRepository(implicit projectId: Int) = new ClassPathTemplateRepository(
        prop(".urls") match {
            case NOT_SET => classLoader
            case _ => new URLClassLoader(prop(".urls").split(",").map(new URL(_)).toArray)
      }, wildcard(projectId), charset(projectId))
    })

  @Bean def gitTemplateRepoFactory = new PullingTemplateRepoFactory(new BaseTemplateRepoFactory(Git) {
    def credentials(implicit projectId: Int) = new Credentials(prop("identity"), prop("credential"))

    def newTemplateRepository(implicit projectId: Int) = new GitTemplateRepository(prop("uri"), credentials, prop("branch"),
      new File(prop(".directory")), wildcard(projectId), charset(projectId))
  })
    
    @Bean def fsRepoFactory = new BaseTemplateRepoFactory(Local) {
      def newTemplateRepository(implicit projectId: Int) = new FilesystemTemplateRepository(prop("path", "fs"), wildcard(projectId))
        with SelfCachingTemplateRepository
    }

  class PullingTemplateRepoFactory(factory: TemplateRepositoryFactory) extends TemplateRepositoryFactory {
    def newTemplateRepository(implicit projectId: Int) = factory.newTemplateRepository(projectId) match {
      case s:TemplateRepository with SelfCachingTemplateRepository => s
      case x => new PullingTemplateRepository(x, pullPeriodSeconds, pullOnStart, cacheManager)
    }

    val mode = factory.mode
  }


  abstract class BaseTemplateRepoFactory(val mode: Mode) extends TemplateRepositoryFactory {
    def prop(suffix: String, modeStr: String = mode.toString)(implicit projectId: Int) = config.get(projectId, Seq(PREFIX, modeStr, suffix).mkString("."), NOT_SET)
  }
}

