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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.plugin

import org.springframework.beans.factory.support.{RootBeanDefinition, BeanDefinitionRegistry, BeanDefinitionRegistryPostProcessor}
import collection.{JavaConversions => JC}
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.core.Ordered
import com.griddynamics.genesis.spring.BeanClassLoaderAware
import java.util.{Properties, UUID}
import org.springframework.core.io.DefaultResourceLoader
import com.griddynamics.genesis.util.{Closeables, InputUtil}

class PluginLoader extends BeanClassLoaderAware with BeanDefinitionRegistryPostProcessor with Ordered {

    import PluginLoader._

    def getOrder = Ordered.HIGHEST_PRECEDENCE

    def postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        if (pluginResourcePath == null) {
            pluginResourcePath = loadGenesisProperties(classLoader)
                .getProperty(PropNamePluginInfo, DefaultPluginResourcePath)
        }
        for (url <- JC.enumerationAsScalaIterator(classLoader.getResources(pluginResourcePath))) {
            for (rawClazz <- InputUtil.getLines(url.openStream())) {
                registerBeanDefinition(rawClazz, registry)
            }
        }
    }

    def registerBeanDefinition(rawClazz: String, registry: BeanDefinitionRegistry) {
        val clazz = rawClazz.trim()

        if (!clazz.isEmpty)
            registry.registerBeanDefinition(clazz + "#" + UUID.randomUUID().toString, new RootBeanDefinition(clazz))
    }

    def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {}
}

object PluginLoader {
    val DefaultPluginResourcePath = "META-INF/genesis/plugin.info"
    val PropNamePluginInfo = "genesis.plugin.info"
    val PropBackend = "backend.properties"
    var pluginResourcePath = java.lang.System.getProperty(PropNamePluginInfo)

    def loadGenesisProperties(classLoader: ClassLoader) = {
        val resourceLoader = new DefaultResourceLoader(classLoader)
        val genesisProperties = new Properties
        Closeables.using(resourceLoader.getResource(java.lang.System.getProperty(PropBackend)).getInputStream) {
            stream => {
                genesisProperties.load(stream)
            }
        }
        genesisProperties
    }
}
