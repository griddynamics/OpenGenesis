/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.repository.ProjectPropertyRepository
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis._

@Controller
@RequestMapping(value = Array("/rest/projects/{projectId}/context"))
class ProjectPropertiesController extends RestApiExceptionsHandler {
  @Autowired
  var projectPropertyRepository: ProjectPropertyRepository = null

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listForProject(@PathVariable("projectId") projectId: Int): List[api.ProjectProperty] = {
    projectPropertyRepository.listForProject(projectId)
  }

  @RequestMapping(method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateForProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = {
    projectPropertyRepository.updateForProject(projectId, extractProperties(request))
  }

    @RequestMapping(method = Array(RequestMethod.POST))
    @ResponseBody
    def create(@PathVariable("projectId") projectId: Int, request: HttpServletRequest)  =
        projectPropertyRepository.create(projectId, extractProperties(request))

    @RequestMapping(method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable("projectId") projectId: Int, request: HttpServletRequest)  =
        projectPropertyRepository.delete(projectId, GenesisRestController.extractParamsList(request))

    def extractProperties(request: HttpServletRequest): List[api.ProjectProperty] = {
        val properties = for (p <- GenesisRestController.extractParamsMapList(request)) yield {
            val name = GenesisRestController.extractValue("name", p)
            val value = GenesisRestController.extractValue("value", p)
            new api.ProjectProperty(0, 0, name, value)
        }
        properties
    }
}
