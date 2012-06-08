package com.griddynamics.genesis.rest

import scala.Array
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import GenesisRestController._
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.spring.ApplicationContextAware
import com.griddynamics.genesis.api.RequestResult

@Controller
@RequestMapping(Array("/rest"))
class RolesController(authorityService: AuthorityService, projectAuthorityService: ProjectAuthorityService)
  extends RestApiExceptionsHandler with ApplicationContextAware {

  private lazy val userService: Option[UserService] = Option(applicationContext.getBean(classOf[UserService]))
  private lazy val groupService: Option[GroupService] = Option(applicationContext.getBean(classOf[GroupService]))

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
  def updateRole(@PathVariable("roleName") roleName: String, request: HttpServletRequest): RequestResult = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException("Role [name = " + roleName + "] was not found")
    }
    val grantsMap = extractParamsMap(request)
    val groups = extractListValue("groups", grantsMap)
    val users = extractListValue("users", grantsMap)
    validUsers(users) {
      validGroups (groups) {
        authorityService.updateAuthority(roleName, groups, users)
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
  private def validUsers[A](usernames: Seq[String])(block: => A): A = {
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
  private def validGroups[A](groupNames: Seq[String])(block: => A): A = {
    groupService.map { service =>
      if(!service.doGroupsExist(groupNames)) {
        throw new ResourceNotFoundException("List of groups contains unknown  groups")
      }
    }
    block
  }
}