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
import java.lang.RuntimeException
import java.io.File
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.spring.{ApplicationContextAware, BeanClassLoaderAware}
import com.griddynamics.genesis.template._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import net.sf.ehcache.CacheManager
import org.springframework.context.annotation.{Lazy, Configuration, Bean}
import java.net.{URLClassLoader, URL}
import com.griddynamics.genesis.plugin.PluginRegistry

@Configuration
class TemplateRepositoryContextImpl extends TemplateRepositoryContext
                                       with BeanClassLoaderAware with ApplicationContextAware with Logging {
    @Value("${genesis.template.repository.git.uri:NOT-SET!!!}") var gitUri: String = _

    @Value("${genesis.template.repository.git.identity:NOT-SET!!!}") var gitIdentity: String = _
    @Value("${genesis.template.repository.git.credential:NOT-SET!!!}") var gitCredential: String = _

    @Value("${genesis.template.repository.git.branch:NOT-SET!!!}") var gitBranch: String = _
    @Value("${genesis.template.repository.git.directory:NOT-SET!!!}") var gitDirectory: String = _

    @Value("${genesis.template.repository.charset:UTF-8}") var charset: String = _
    @Value("${genesis.template.repository.wildcard:*.genesis}") var wildcard: String = _

    @Value("${genesis.template.repository.mode:classpath}") var mode: String = _
    @Value("${genesis.template.repository.fs.path:NOT-SET!!!}") var path: String = null
    @Value("${genesis.template.repository.classpath.urls:NOT-SET!!!}") var urls: String = null

    @Value("${genesis.template.repository.pull.period.seconds:3600}") var pullPeriodSeconds: Long = _
    @Value("${genesis.template.repository.pull.on.start:true}") var pullOnStart: Boolean = _

    @Autowired var cacheManager : CacheManager = _

    @Autowired var pluginRegistry: PluginRegistry = _

    @Bean
    def templateRepository = {
        import collection.JavaConversions._
        val m = Modes.withName(mode.toLowerCase)
        val drivers = for ((name, bean) <- mapAsScalaMap(applicationContext.getBeansOfType(classOf[ModeAwareTemplateRepository])) if
            bean.respondTo == m) yield bean
        val result = drivers.headOption match {
            case Some(driver) => {
                if (driver.isInstanceOf[SelfCachingTemplateRepository])
                    driver
                else
                    new PullingTemplateRepository(driver, pullPeriodSeconds, pullOnStart, cacheManager)
            }
            case None =>
                val plugins = pluginRegistry.getPlugins(classOf[ModeAwareTemplateRepository])
                plugins.values.find( _.respondTo == m).getOrElse(
                throw new RuntimeException("Unknown repository mode %s".format(mode)))
        }
        result
    }

    @Bean def classPathTemplateRepository = {
        urls match {
            case "NOT-SET!!!" => new ClassPathTemplateRepository(classLoader, wildcard, charset)
            case _ => new ClassPathTemplateRepository(new URLClassLoader(urls.split(",").map(new URL(_)).toArray), wildcard, charset)
        }
    }

    @Bean def gitTemplateRepository = {
        val credentials = new Credentials(gitIdentity, gitCredential)

        new GitTemplateRepository(gitUri, credentials, gitBranch,
                                  new File(gitDirectory), wildcard, charset)
    }
    
    @Bean @Lazy def filesystemRepository = new FilesystemTemplateRepository(path, wildcard) with SelfCachingTemplateRepository
}
