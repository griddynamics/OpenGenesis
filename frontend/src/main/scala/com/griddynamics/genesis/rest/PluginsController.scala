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
package com.griddynamics.genesis.rest

import annotations.LinkTarget._
import links._
import HrefBuilder._
import CollectionWrapper._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.ConfigPropertyType
import com.griddynamics.genesis.plugin.PluginRepository
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.api.PluginDetails
import com.griddynamics.genesis.api.Plugin
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(value = Array("/rest/plugins"))
class PluginsController extends RestApiExceptionsHandler  {

  import ConfigPasswordHelper._

  @Autowired var repository: PluginRepository = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listPlugins(request: HttpServletRequest): CollectionWrapper[ItemWrapper[Plugin]] = {
    implicit val req = request
    wrapCollection(repository.listPlugins.toList.map(plugin => {
       val top = WebPath(request)
       wrap(plugin).withLinks(LinkBuilder(top / plugin.id, SELF, classOf[Plugin], GET, PUT)).filtered()
    })).withLinksToSelf(classOf[Plugin], GET).filtered()
  }

  @RequestMapping(value = Array("{pluginId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getPluginDescription(@PathVariable("pluginId") pluginId: String, request: HttpServletRequest): ItemWrapper[PluginDetails] ={
    val plugin = repository.getPlugin(pluginId).getOrElse(throw new ResourceNotFoundException("Plugin [id = " + pluginId + "] was not found"))
    wrap(plugin.copy(configuration = hidePasswords(plugin.configuration))).
      withLinks(LinkBuilder(request, SELF, classOf[PluginDetails], GET, PUT)).filtered()
  }

  @RequestMapping(value = Array("{pluginId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updatePluginConfiguration(@PathVariable("pluginId") pluginId: String, request: HttpServletRequest) = {
    val plugin = repository.getPlugin(pluginId).getOrElse(throw new ResourceNotFoundException("Plugin [id = " + pluginId + "] was not found"))
    val pluginConfig = plugin.configuration.filterNot(_.readOnly).map(item => (item.name, item.propertyType)).toMap
    
    val paramsMap = GenesisRestController.extractParamsMap(request)

    val propertiesList = paramsMap
      .getOrElse("configuration", throw new MissingParameterException("configuration"))
      .asInstanceOf[List[Map[String,String]]]

    val updatedConfigs = for {
      property  <- propertiesList
      name <- property.get("name")
      value <- property.get("value")
      if (pluginConfig.isDefinedAt(name))
      if (pluginConfig(name) != ConfigPropertyType.PASSWORD || value != blankPassword)
    } yield (property("name"), property("value").trim)

    repository.updateConfiguration(plugin.id, updatedConfigs.toMap)
  }

}
