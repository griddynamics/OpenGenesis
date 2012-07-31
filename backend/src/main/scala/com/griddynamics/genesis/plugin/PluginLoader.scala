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
package com.griddynamics.genesis.plugin

import api.GenesisPlugin
import org.springframework.beans.factory.support.{RootBeanDefinition, BeanDefinitionRegistry, BeanDefinitionRegistryPostProcessor}
import collection.{JavaConversions => JC}
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.core.Ordered
import com.griddynamics.genesis.spring.BeanClassLoaderAware
import org.springframework.core.io.DefaultResourceLoader
import com.griddynamics.genesis.util.{Closeables, InputUtil}
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.`type`.filter.AnnotationTypeFilter
import collection.JavaConversions._
import java.util.{Properties, UUID}

class PluginLoader extends BeanClassLoaderAware with BeanDefinitionRegistryPostProcessor with Ordered {

    import PluginLoader._

    private var plugins: Set[GenesisPlugin] = _

    def getOrder = Ordered.HIGHEST_PRECEDENCE


    private def scanForAnnotatedPlugins(classNames: Iterator[String], registry: BeanDefinitionRegistry) = {
        val scanner = new ClassPathBeanDefinitionScanner(registry, false)
        scanner.addIncludeFilter(new AnnotationTypeFilter(classOf[GenesisPlugin]))

        val components = scanner.findCandidateComponents("com.griddynamics.genesis.configuration").map(_.getBeanClassName)

        (components ++ classNames).map {
            beanClassName => Option(Class.forName(beanClassName).getAnnotation(classOf[GenesisPlugin]))
        }.flatten
    }

    def postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        loadGenesisProperties(classLoader)

        val resources = classLoader.getResources(pluginResourcePath)

        val classNames = for {
          url <- JC.enumerationAsScalaIterator(resources)
          rawClazz <- InputUtil.getLines(url.openStream())
          if !rawClazz.trim.isEmpty
        } yield registerBeanDefinition(rawClazz, registry)

        this.plugins = scanForAnnotatedPlugins(classNames, registry).toSet
    }

    def registerBeanDefinition(rawClazz: String, registry: BeanDefinitionRegistry): String = {
        val clazz = rawClazz.trim()
        registry.registerBeanDefinition(clazz + "#" + UUID.randomUUID().toString, new RootBeanDefinition(clazz))
        clazz
    }

    def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {}

    def loadedPlugins = plugins
}

object PluginLoader {
    import com.griddynamics.genesis.service.GenesisSystemProperties.BACKEND
    var pluginResourcePath = "META-INF/genesis/plugin.info"

    def loadGenesisProperties(classLoader: ClassLoader) = {
        val resourceLoader = new DefaultResourceLoader(classLoader)
        val genesisProperties = new Properties
        Closeables.using(resourceLoader.getResource(java.lang.System.getProperty(BACKEND)).getInputStream) {
            stream => {
                genesisProperties.load(stream)
            }
        }
        genesisProperties
    }
}
