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

import api.GenesisPlugin
import com.griddynamics.genesis
import genesis.api.{ExtendedResult, PluginDetails}
import collection.immutable.Map
import genesis.service.{GenesisSystemProperties, ConfigService}
import org.springframework.transaction.annotation.Transactional

trait PluginRepository {
  def getPlugin(id: String): Option[genesis.api.PluginDetails]
  def updateConfiguration(pluginId: String, configuration: Map[String, Any]): ExtendedResult[_]
  def listPlugins: Iterable[genesis.api.Plugin]
}

class PluginRepositoryImpl(pluginLoader: PluginLoader,
                       configService: ConfigService) extends PluginRepository with PluginConfigurationContext {

  private val plugins: Map[String, GenesisPlugin] = pluginLoader.loadedPlugins.map(plugin => (plugin.id(), plugin)).toMap

  lazy val listPlugins: Iterable[genesis.api.Plugin] =
    plugins.values.map(annotation => genesis.api.Plugin(annotation.id, Option(annotation.description)))


  def getPlugin(id: String): Option[genesis.api.PluginDetails] = {
    plugins.get(id).map { plugin =>
      PluginDetails(plugin.id, Option(plugin.description), configService.listSettings(Some(GenesisSystemProperties.PLUGIN_PREFIX + "." + id)))
    }
  }

  def updateConfiguration(pluginId: String, configuration: Map[String, Any]) = configService.update(configuration)

  def configuration(pluginId: String): Map[String, String] = {
    val settingsKey = Some(GenesisSystemProperties.PLUGIN_PREFIX + "." + pluginId)
    configService.listSettings(settingsKey).map { property => (property.name, property.value) }.toMap
  }
}

