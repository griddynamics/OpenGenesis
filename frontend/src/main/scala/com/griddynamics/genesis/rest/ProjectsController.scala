package com.griddynamics.genesis.rest

import annotations.{LinkTo, LinksTo}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.rest.GenesisRestController._
import links.CollectionWrapper
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import com.griddynamics.genesis.service.impl.ProjectService
import com.griddynamics.genesis.api._
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.Authentication
import com.griddynamics.genesis.users.{UserService, GenesisRole}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.griddynamics.genesis.service.ProjectAuthorityService
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.Project
import javax.validation.Valid
import com.griddynamics.genesis.repository.ConfigurationRepository
import com.griddynamics.genesis.groups.GroupService

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
  @Autowired var configurationRepository: ConfigurationRepository = _
  @Autowired var userService: UserService = _
  @Autowired var groupService: GroupService = _

  @Value("${genesis.system.server.mode:frontend}")
  var mode = ""

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  @LinksTo(value = Array(new LinkTo(methods = Array(RequestMethod.POST), clazz = classOf[Project], controller = classOf[ProjectsController])))
  def listProjects(@RequestParam(value = "sorting", required = false, defaultValue = "name") sorting: Ordering,
                   request: HttpServletRequest): CollectionWrapper[Project] = {
    if (request.isUserInRole(GenesisRole.SystemAdmin.toString) || request.isUserInRole(GenesisRole.ReadonlySystemAdmin.toString)) {
      projectService.orderedList(sorting)
    } else {
      val authorities = GenesisRestController.getCurrentUserAuthorities
      val ids = authorityService.getAllowedProjectIds(request.getUserPrincipal.getName, authorities)
      projectService.getProjects(ids, Option(sorting))
    }
  }


  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def createProject(@Valid @RequestBody attr: ProjectAttributes) = {
    val project = Project(
      id = None,
      name = attr.name.trim,
      projectManager = attr.projectManager.trim,
      creator = getCurrentUser,
      creationTime = System.currentTimeMillis(),
      description = attr.description.map(_.trim)
    )

    val result = projectService.create(project)
    result match {
      case r@Success(pr) => {
        configurationRepository.save(new Configuration(None, "Default", pr.id.get, None))
        r
      }
      case r => r
    }
  }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def findProject(@PathVariable("projectId") projectId: Int): Project =
    projectService.get(projectId).getOrElse { throw new ResourceNotFoundException("Project [id = " + projectId + "]  was not found") }


  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateProject(@PathVariable("projectId") projectId: Int, @Valid @RequestBody attr: ProjectAttributes) = {
    val project = findProject(projectId).copy(name = attr.name.trim, projectManager =  attr.projectManager.trim, description = attr.description.map(_.trim))
    projectService.update(project)
  }

  @RequestMapping(value = Array("{projectId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteProject(@PathVariable("projectId") projectId: Int) = {
    val project = findProject(projectId)
    projectService.delete(project)
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

    val nonExistentUsers = users.map(_.toLowerCase).toSet -- userService.findByUsernames(users).map(_.username.toLowerCase)
    val nonExistentGroups = groups.map(_.toLowerCase).toSet -- groupService.findByNames(groups).map(_.name.toLowerCase)

    authorityService.updateProjectAuthority(projectId,
      GenesisRole.withName(roleName),
      users.distinct,
      groups.distinct)

    Success(
      Map(
        "nonExistentUsers" -> nonExistentUsers,
        "nonExistentGroups" -> nonExistentGroups
      )
    )
  }

  @RequestMapping(value = Array("{projectId}/roles/{roleName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def loadProjectAuths(@PathVariable("projectId") projectId: Int,
                       @PathVariable("roleName") roleName: String,
                       request: HttpServletRequest,
                       response: HttpServletResponse): ExtendedResult[Map[String, Any]] = {
    authorityService.getProjectAuthority(projectId, GenesisRole.withName(roleName)).map{ case (users, groups) =>
      Map(
        "users" -> Users.of(userService).forUsernames(users),
        "groups" -> groups
      )
    }
  }

  @RequestMapping(value = Array("{projectId}/permissions"), method = Array(RequestMethod.GET))
  @ResponseBody
  def permissions(@PathVariable("projectId") projectId: Int,
                       request: HttpServletRequest,
                       response: HttpServletResponse): Seq[String] = {
    import scala.collection.JavaConversions._
    if (request.isUserInRole(GenesisRole.SystemAdmin.toString) || request.isUserInRole(GenesisRole.ReadonlySystemAdmin.toString)) {
      List(GenesisRole.ProjectAdmin.toString, GenesisRole.ProjectUser.toString)
    } else {
      val auth: Authentication = SecurityContextHolder.getContext.getAuthentication
      val authorities = auth.getAuthorities.map (_.getAuthority)
      authorityService.getGrantedAuthorities(projectId, request.getUserPrincipal.getName, authorities).map(_.toString)
    }
  }
}
