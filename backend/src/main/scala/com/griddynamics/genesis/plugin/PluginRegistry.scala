/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.plugin

import api.{PluginInstanceFactory, GenesisPlugin}
import com.griddynamics.genesis.spring.ApplicationContextAware
import collection.JavaConversions._
import javax.annotation.PostConstruct
import com.griddynamics.genesis.service.ConfigService
import org.springframework.core.annotation.AnnotationUtils._

trait PluginRegistry {
  def getPlugins[B <: AnyRef](clazz: Class[B]): Map[GenesisPlugin, B]
}

class PluginRegistryImpl(configService: ConfigService) extends PluginRegistry with ApplicationContextAware {

  private var factories: Iterable[PluginInstanceFactory[_]] = _

  @PostConstruct
  def init() {
    this.factories = applicationContext
      .getBeansOfType(classOf[PluginInstanceFactory[_]])
      .values
      .filter { factory => findAnnotation(factory.getClass, classOf[GenesisPlugin]) != null }
  }

  def getPlugins[B <: AnyRef](clazz: Class[B]): Map[GenesisPlugin,B] = {
    val beans = applicationContext.getBeansOfType(clazz).values

    val loadedPlugins = for {
      bean: B <- beans
      pluginMeta <- Option(findAnnotation(bean.getClass, classOf[GenesisPlugin]))
    } yield (pluginMeta, bean)

    if (!loadedPlugins.isEmpty) loadedPlugins.toMap else tryFactories(clazz)
  }

  private def tryFactories[B <: AnyRef](clazz: Class[B]): Map[GenesisPlugin, B] = {
    //todo: FIXME plugin should not receive all properties.change this after all plugins migrated
    val systemConfig = configService.listSettings(None).map { property => (property.name, property.value) }.toMap

    val fact = factories.filter { _.pluginClazz.isAssignableFrom(clazz) }.map { _.asInstanceOf[PluginInstanceFactory[B]] }
    fact.map { factory =>
      val metadata = findAnnotation(factory.getClass, classOf[GenesisPlugin])
      val bean = factory.create(systemConfig)

      (metadata, bean)
    }.toMap
  }
}
