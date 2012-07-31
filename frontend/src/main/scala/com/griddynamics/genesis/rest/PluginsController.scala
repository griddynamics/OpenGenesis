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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.{PluginDetails, Plugin}
import com.griddynamics.genesis.plugin.PluginRepository
import org.springframework.beans.factory.annotation.Autowired

@Controller
@RequestMapping(value = Array("/rest/plugins"))
class PluginsController extends RestApiExceptionsHandler {
  @Autowired var repository: PluginRepository = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listPlugins: List[Plugin] = repository.listPlugins.toList

  @RequestMapping(value = Array("{pluginId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getPluginDescription(@PathVariable("pluginId") pluginId: String): PluginDetails =
    repository.getPlugin(pluginId).getOrElse(throw new ResourceNotFoundException("Plugin [id = " + pluginId + "] was not found"))


  @RequestMapping(value = Array("{pluginId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updatePluginConfiguration(@PathVariable("pluginId") pluginId: String, request: HttpServletRequest) {
    val plugin = repository.getPlugin(pluginId).getOrElse(throw new ResourceNotFoundException("Plugin [id = " + pluginId + "] was not found"))
    val pluginConfig = plugin.configuration.filterNot(_.readOnly).map(item => (item.name, item.value)).toMap
    
    val paramsMap = GenesisRestController.extractParamsMap(request)

    val propertiesList = paramsMap
      .getOrElse("configuration", throw new MissingParameterException("configuration"))
      .asInstanceOf[List[Map[String,String]]]

    val updatedConfigs = for {
      property  <- propertiesList
      if (pluginConfig.isDefinedAt(property("name")))
    } yield (property("name"), property("value"))

    repository.updateConfiguration(plugin.id, updatedConfigs.toMap)
  }

}
