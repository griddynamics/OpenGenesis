package com.griddynamics.genesis.rest

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import com.griddynamics.genesis.service.impl.ProjectService
import com.griddynamics.genesis.api.Project

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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
@Controller
@RequestMapping(value = Array("/rest/projects"))
class ProjectsController(projectService: ProjectService) extends RestApiExceptionsHandler {
  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listProjects: List[Project] = projectService.list.toList.sortWith(_.name.toLowerCase < _.name.toLowerCase)

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def createProject(request: HttpServletRequest, response: HttpServletResponse) = {
    val paramsMap = extractParamsMap(request)
    val project = extractProject(None, paramsMap)
    projectService.create(project)
  }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def findProject(@PathVariable("projectId") projectId: Int): Project =
    projectService.get(projectId).getOrElse { throw new ResourceNotFoundException }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest, response: HttpServletResponse) = {
    val paramsMap = GenesisRestController.extractParamsMap(request)
    val project = extractProject(Option(projectId), paramsMap)
    projectService.get(projectId) match {
        case Some(p) => projectService.update(project)
        case _ => throw new ResourceNotFoundException
    }
  }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest, response: HttpServletResponse) {
    projectService.get(projectId).foreach( projectService.delete(_) )
  }

  private def extractProject(projectId: Option[Int], paramsMap: Map[String, Any]): Project = {
    val name = extractNotEmptyValue("name", paramsMap)
    val projectManager = extractNotEmptyValue("projectManager", paramsMap)
    val description = extractOption("description", paramsMap)

    new Project(projectId, name, description, projectManager)
  }
}
