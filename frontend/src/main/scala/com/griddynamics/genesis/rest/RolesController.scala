/*
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
import links.{WebPath, ItemWrapper, CollectionWrapper}
import links.CollectionWrapper._
import links.WebPath._
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import org.springframework.web.bind.annotation.RequestMethod._
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import GenesisRestController._
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.api._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest"))
class RolesController extends RestApiExceptionsHandler {

  @Autowired var authorityService: AuthorityService = _
  @Autowired var projectAuthorityService: ProjectAuthorityService= _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @Autowired var userService: UserService = _
  @Autowired var groupService: GroupService = _

  @RequestMapping(value = Array("roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET), modelClass = classOf[ApplicationRole])
  def listSystemRoles(request: HttpServletRequest) : CollectionWrapper[ItemWrapper[ApplicationRole]] = {
    val builtRoles = authorityService.listAuthorities.map(s => ApplicationRole(s))
    builtRoles.map { role => role.withLinksToSelf(request / role.name, GET, PUT).filtered() }
  }

  @RequestMapping(value = Array("projectRoles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def listProjectRoles() = projectAuthorityService.projectAuthorities

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def userRoles(@PathVariable("username")username: String): List[String] = authorityService.getUserAuthorities(username)

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateUserRoles(@PathVariable("username")username: String, request: HttpServletRequest) = {
    val roles = GenesisRestController.extractParamsList(request)
    if (username.matches(validADUserName))
      authorityService.grantAuthoritiesToUser(username, roles)
    else
      Failure(compoundServiceErrors = List(ADUserNameErrorMessage.format(username)))
  }

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def groupRoles(@PathVariable("groupName") groupName: String): List[String] = authorityService.getGroupsAuthorities(groupName)

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateGroupRoles(@PathVariable("groupName") groupName: String, request: HttpServletRequest): ExtendedResult[_] = {
    val roles = GenesisRestController.extractParamsList(request)
    if (groupName.matches(validADGroupName))
      authorityService.grantAuthoritiesToGroup(groupName, roles)
    else
      Failure(compoundServiceErrors = List(ADGroupNameErrorMessage.format(groupName)))
  }

  @RequestMapping(value = Array("roles/{roleName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeRole(@PathVariable("roleName") roleName: String) = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException("Role [name = " + roleName + "] was not found")
    }

    val associations = authorityService.authorityAssociations(roleName)

    Map(
      "name" -> associations.name,
      "groups" -> associations.groups,
      "users" -> Users.of(userService).forUsernames(associations.users)
    )
  }

  @RequestMapping(value = Array("roles/{roleName}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateRole(@PathVariable("roleName") roleName: String, request: HttpServletRequest): ExtendedResult[Any] = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException("Role [name = " + roleName + "] was not found")
    }
    val grantsMap = extractParamsMap(request)

    val groups = extractListValue("groups", grantsMap)
    val users = extractListValue("users", grantsMap)

    val invalidUsers = users.filterNot(_.matches(validADUserName))
    val invalidGroups = groups.filterNot(_.matches(validADGroupName))

    if(invalidGroups.nonEmpty || invalidUsers.nonEmpty) {
      return Failure(
        compoundServiceErrors = invalidUsers.map(ADUserNameErrorMessage.format(_)) ++ invalidGroups.map(ADGroupNameErrorMessage.format(_))
      )
    }

    val nonExistentUsers = users.map(_.toLowerCase).toSet -- userService.findByUsernames(users).map(_.username.toLowerCase)
    val nonExistentGroups = groups.map(_.toLowerCase).toSet -- groupService.findByNames(groups).map(_.name.toLowerCase)

    authorityService.updateAuthority(roleName, groups.distinct, users.distinct)

    Success(
      Map(
        "nonExistentUsers" -> nonExistentUsers,
        "nonExistentGroups" -> nonExistentGroups
      )
    )
  }

}

