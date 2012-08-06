package com.griddynamics.genesis.rest

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import com.griddynamics.genesis.service.impl.ProjectService
import com.griddynamics.genesis.api._
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.Authentication
import com.griddynamics.genesis.users.GenesisRole
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.griddynamics.genesis.service.ProjectAuthorityService
import com.griddynamics.genesis.validation.Validation
import scala.Some
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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
@Controller
@RequestMapping(value = Array("/rest/projects"))
class ProjectsController extends RestApiExceptionsHandler {


  @Autowired var projectService: ProjectService = _
  @Autowired var authorityService: ProjectAuthorityService = _

  @Value("${genesis.system.server.mode:frontend}")
  var mode = ""

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listProjects(request: HttpServletRequest): Iterable[Project] = {
    import scala.collection.JavaConversions._
    if (request.isUserInRole(GenesisRole.SystemAdmin.toString)) {
      projectService.list
    } else {
      val auth = SecurityContextHolder.getContext.getAuthentication
      val authorities = auth.getAuthorities.map (_.getAuthority)
      val ids = authorityService.getAllowedProjectIds(request.getUserPrincipal.getName, authorities)
      projectService.getProjects(ids)
    }
  }

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
    projectService.get(projectId).getOrElse { throw new ResourceNotFoundException("Project [id = " + projectId + "]  was not found") }


  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest, response: HttpServletResponse) = {
    val paramsMap = GenesisRestController.extractParamsMap(request)
    val project = extractProject(Option(projectId), paramsMap)
    projectService.get(projectId) match {
      case Some(p) => projectService.update(project)
      case _ => throw new ResourceNotFoundException("Project [id = " + projectId + "]  was not found")
    }
  }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest, response: HttpServletResponse) = {
    projectService.get(projectId) match {
      case Some(project) => projectService.delete(project)
      case _ => throw new ResourceNotFoundException("Project [id = %s] was not found".format(projectId))
    }
  }

  private def extractProject(projectId: Option[Int], paramsMap: Map[String, Any]): Project = {
    val name = extractNotEmptyValue("name", paramsMap)
    val projectManager = extractNotEmptyValue("projectManager", paramsMap)
    val description = extractOption("description", paramsMap)

    new Project(projectId, name, description, projectManager)
  }

  @RequestMapping(value = Array("{projectId}/roles/{roleName}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateProjectRole(@PathVariable("projectId") projectId: Int,
                        @PathVariable("roleName") roleName: String,
                        request: HttpServletRequest,
                        response: HttpServletResponse): ExtendedResult[_] = {
    val paramsMap = GenesisRestController.extractParamsMap(request)

    val users = GenesisRestController.extractListValue("users", paramsMap)
    val groups = GenesisRestController.extractListValue("groups", paramsMap)

    import Validation._
    val invalidUsers = users.filterNot(_.matches(validADUserName))
    val invalidGroups = groups.filterNot(_.matches(validADGroupName))

    if(invalidGroups.nonEmpty || invalidUsers.nonEmpty) {
      return Failure(
        compoundServiceErrors = invalidUsers.map(ADUserNameErrorMessage.format(_)) ++ invalidGroups.map(ADGroupNameErrorMessage.format(_))
      )
    }

    authorityService.updateProjectAuthority(projectId,
      GenesisRole.withName(roleName),
      users.distinct,
      groups.distinct)
  }

  @RequestMapping(value = Array("{projectId}/roles/{roleName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def loadProjectAuths(@PathVariable("projectId") projectId: Int,
                       @PathVariable("roleName") roleName: String,
                       request: HttpServletRequest,
                       response: HttpServletResponse): ExtendedResult[Map[String, Iterable[String]]] = {
    authorityService.getProjectAuthority(projectId, GenesisRole.withName(roleName)).map{ case (users, groups) => Map("users" -> users, "groups" -> groups) }
  }

  @RequestMapping(value = Array("{projectId}/permissions"), method = Array(RequestMethod.GET))
  @ResponseBody
  def permissions(@PathVariable("projectId") projectId: Int,
                       request: HttpServletRequest,
                       response: HttpServletResponse): List[String] = {
    import scala.collection.JavaConversions._
    if (request.isUserInRole(GenesisRole.SystemAdmin.toString)) {
      List(GenesisRole.ProjectAdmin.toString, GenesisRole.ProjectUser.toString)
    } else {
      val auth: Authentication = SecurityContextHolder.getContext.getAuthentication
      val authorities = auth.getAuthorities.map (_.getAuthority)
      authorityService.getGrantedAuthorities(projectId, request.getUserPrincipal.getName, authorities).map(_.toString)
    }
  }
}
