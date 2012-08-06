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

import scala.Array
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import GenesisRestController._
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.api.{Failure, ExtendedResult, RequestResult}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.validation.Validation

@Controller
@RequestMapping(Array("/rest"))
class RolesController extends RestApiExceptionsHandler {

  @Autowired var authorityService: AuthorityService = _
  @Autowired var  projectAuthorityService: ProjectAuthorityService= _

  @Autowired(required = false) var userServiceBean: UserService = _
  @Autowired(required = false) var groupServiceBean: GroupService = _
  private lazy val userService: Option[UserService] = Option(userServiceBean)
  private lazy val groupService: Option[GroupService] = Option(groupServiceBean)

  @RequestMapping(value = Array("roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def listSystemRoles() = authorityService.listAuthorities

  @RequestMapping(value = Array("projectRoles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def listProjectRoles() = projectAuthorityService.projectAuthorities

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def userRoles(@PathVariable("username")username: String): List[String] = validUser(username) { authorityService.getUserAuthorities(username) }

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateUserRoles(@PathVariable("username")username: String, request: HttpServletRequest): RequestResult = validUser(username) {
    val roles = GenesisRestController.extractParamsList(request)
    authorityService.grantAuthoritiesToUser(username, roles)
  }

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def groupRoles(@PathVariable("groupName") groupName: String): List[String] = validGroup(groupName) { authorityService.getGroupAuthorities(groupName) }

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateGroupRoles(@PathVariable("groupName") groupName: String, request: HttpServletRequest) = validGroup(groupName) {
    val roles = GenesisRestController.extractParamsList(request)
    authorityService.grantAuthoritiesToGroup(groupName, roles)
  }

  @RequestMapping(value = Array("roles/{roleName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeRole(@PathVariable("roleName") roleName: String) = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException("Role [name = " + roleName + "] was not found")
    }
    authorityService.authorityAssociations(roleName)
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

    import Validation._
    val invalidUsers = users.filterNot(_.matches(validADUserName))
    val invalidGroups = groups.filterNot(_.matches(validADGroupName))

    if(invalidGroups.nonEmpty || invalidUsers.nonEmpty) {
      return Failure(
        compoundServiceErrors = invalidUsers.map(ADUserNameErrorMessage.format(_)) ++ invalidGroups.map(ADGroupNameErrorMessage.format(_))
      )
    }

    validUsers(users) {
      validGroups (groups) {
        authorityService.updateAuthority(roleName, groups.distinct, users.distinct)
      }
    }
  }

  private def validUser[A](username: String)(block: => A): A = {
    userService.map { service =>
      if (!service.doesUserExist(username)) {
        throw new ResourceNotFoundException("User [username=" + username + "] was not found")
      }
    }
    block
  }
  private def validUsers[A](usernames: Seq[String])(block: => ExtendedResult[_]): ExtendedResult[_] = {
    userService.map { service =>
      if (!service.doUsersExist(usernames)) {
        throw new ResourceNotFoundException("List of users contains unknown usernames")
      }
    }
    block
  }

  private def validGroup[A](groupName: String)(block: => A): A = {
    groupService.map { service =>
      if (!service.doesGroupExist(groupName)) {
        throw new ResourceNotFoundException("Group [name = " + groupName + "] was not found")
      }
    }
    block
  }
  private def validGroups[A](groupNames: Seq[String])(block: => ExtendedResult[_]): ExtendedResult[_] = {
    groupService.map { service =>
      if(!service.doGroupsExist(groupNames)) {
        throw new ResourceNotFoundException("List of groups contains unknown  groups")
      }
    }
    block
  }
}

