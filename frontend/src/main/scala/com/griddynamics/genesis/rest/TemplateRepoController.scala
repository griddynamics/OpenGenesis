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
package com.griddynamics.genesis.rest

import annotations.AddSelfLinks
import links.ItemWrapper
import links.CollectionWrapper._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import javax.servlet.http.HttpServletRequest
import scala.Array
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{TemplateService, TemplateRepoService}
import com.griddynamics.genesis.template.Modes
import com.griddynamics.genesis.api.TemplateRepo

@Controller
@RequestMapping(value = Array("/rest"))
class TemplateRepoController extends RestApiExceptionsHandler {

  @Autowired var service: TemplateRepoService = _
  @Autowired var templateService : TemplateService = _

  @RequestMapping(value = Array("template/repository/modes"),method = Array(RequestMethod.GET))
  @ResponseBody
  def listModes(request: HttpServletRequest) = service.listModes.map(_.toString)

  @RequestMapping(value = Array("template/repository/modes/{mode}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getModeSettings(@PathVariable("mode") mode: String) = try {
    service.listSettings(Modes.withName(mode))
  } catch {
    case t: Throwable => throw new ResourceNotFoundException("No such template repository: %s".format(mode))
  }

  @RequestMapping(value = Array("projects/{projectId}/template/repository"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT), modelClass = classOf[TemplateRepo])
  def getConfig(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) : ItemWrapper[TemplateRepo] = service.getConfig(projectId)

  @RequestMapping(value = Array("projects/{projectId}/template/repository"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) {
    service.updateConfig(projectId, GenesisRestController.extractParamsMap(request))
    templateService.clearCache(projectId) // TODO: think of better way to notify template service of changes
  }

}
