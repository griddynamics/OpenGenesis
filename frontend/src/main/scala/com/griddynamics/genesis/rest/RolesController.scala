package com.griddynamics.genesis.rest

import com.griddynamics.genesis.service.AuthorityService
import scala.Array
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import GenesisRestController._

@Controller
@RequestMapping(Array("/rest"))
class RolesController(authorityService: AuthorityService) extends RestApiExceptionsHandler {

  @RequestMapping(value = Array("roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def list() = authorityService.listAuthorities

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def userRoles(@PathVariable("username")username: String) = authorityService.getUserAuthorities(username)

  @RequestMapping(value = Array("users/{username}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateUserRoles(@PathVariable("username")username: String, request: HttpServletRequest) = {
    val roles = GenesisRestController.extractParamsList(request)
    authorityService.grantAuthoritiesToUser(username, roles)
  }

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.GET))
  @ResponseBody
  def groupRoles(@PathVariable("groupName") groupName: String) = authorityService.getGroupAuthorities(groupName)

  @RequestMapping(value = Array("groups/{groupName}/roles"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateGroupRoles(@PathVariable("groupName") groupName: String, request: HttpServletRequest) = {
    val roles = GenesisRestController.extractParamsList(request)
    authorityService.grantAuthoritiesToGroup(groupName, roles)
  }

  @RequestMapping(value = Array("roles/{roleName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeRole(@PathVariable("roleName") roleName: String) = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException()
    }
    authorityService.authorityAssociations(roleName)
  }

  @RequestMapping(value = Array("roles/{roleName}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateRole(@PathVariable("roleName") roleName: String, request: HttpServletRequest) = {
    if(!authorityService.listAuthorities.contains(roleName)) {
      throw new ResourceNotFoundException()
    }
    val grantsMap = extractParamsMap(request)
    val groups = extractListValue("groups", grantsMap)
    val users = extractListValue("users", grantsMap)
    authorityService.updateAuthority(roleName, groups, users)
  }
}