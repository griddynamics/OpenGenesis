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
 *   Project:     Genesis
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.rest

import annotations.{AddSelfLinks, LinkTarget}
import links.{WebPath, LinkBuilder, ItemWrapper, CollectionWrapper}
import links.CollectionWrapper._
import links.HrefBuilder._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.repository.ConfigurationRepository
import scala.Array
import javax.validation.Valid
import com.griddynamics.genesis.api._
import org.springframework.security.access.prepost.PostFilter
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.service.{EnvironmentConfigurationService, StoreService, EnvironmentAccessService}
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.api.Failure
import scala.Some
import com.griddynamics.genesis.api.Configuration
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/configs"))
class ConfigurationController extends RestApiExceptionsHandler{

  @Autowired var configRepository: ConfigurationRepository = _
  @Autowired var envAuthService: EnvironmentAccessService = _
  @Autowired var storeService: StoreService = _
  @Autowired var userService: UserService = _
  @Autowired var groupService: GroupService = _
  @Autowired var envConfigService: EnvironmentConfigurationService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _


  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @PostFilter("not(@environmentSecurity.restrictionsEnabled()) " +
    "or hasRole('ROLE_GENESIS_ADMIN') or hasRole('ROLE_GENESIS_READONLY')" +
    "or hasPermission( #projectId, 'com.griddynamics.genesis.api.Project', 'administration') " +
    "or hasPermission(filterObject, 'read')")
  @AddSelfLinks(methods = Array(GET, POST), modelClass = classOf[Configuration])
  def list(@PathVariable("projectId") projectId: Int,
           @RequestParam(value = "sorting", required = false, defaultValue = "name") ordering: Ordering,
           request: HttpServletRequest): CollectionWrapper[ItemWrapper[Configuration]] = {
    val permitedConfigs = envConfigService.list(projectId).map(_.id.get).toSet
    val createEnvPath = WebPath(absolutePath("/rest")(request)) / "projects" / projectId / "envs"

    def wrapConfig(config: Configuration) = {
      val id = config.id.get
      val createLink = if (permitedConfigs.contains(id))
        Seq(LinkBuilder(createEnvPath, LinkTarget.ACTION, classOf[Environment], POST))
      else
        Seq()

      val top = WebPath(request)
      config.withLinks(
        LinkBuilder(top / id.toString, LinkTarget.SELF, classOf[Configuration], GET, PUT, DELETE), createLink: _*
      ).filtered()
    }
    configRepository.list(projectId, ordering).map(wrapConfig(_))
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT, DELETE), modelClass = classOf[Configuration])
  def get(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, request: HttpServletRequest): ItemWrapper[Configuration] = {
    val item: ItemWrapper[Configuration] =
      configRepository.get(projectId, id).getOrElse(throw new ResourceNotFoundException("Can not find environment configuration id = %d".format(id)))
    if (envAuthService.restrictionsEnabled)
      item.withLinks(LinkBuilder(WebPath(request) / "access", LinkTarget.COLLECTION, classOf[Access], GET, PUT)).filtered()
    else
      item
  }

  @ResponseBody
  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  def create(@PathVariable("projectId") projectId: Int, @Valid @RequestBody config: Configuration) =
    envConfigService.save(config.copy(id = None, projectId = projectId))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, @Valid @RequestBody config: Configuration) =
    envConfigService.update(config.copy(id = Some(id), projectId = projectId))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int):ExtendedResult[_] = {
    val envIds = storeService.findLiveEnvsByConfigurationId(id)
    if (!envIds.isEmpty){
      return Failure(compoundServiceErrors = Seq("Environment configuration can not be deleted while not all instances are destroyed"))
    }

    if (configRepository.list(projectId).filter(_.id != Some(id)).isEmpty) {
      return Failure(compoundServiceErrors = Seq("At least one environment must be defined in project"))
    }

    if (configRepository.delete(projectId, id) == 1) {
      Success(id)
    } else {
      Failure(compoundServiceErrors = Seq("Environment configuration was not found"))
    }
  }

  @RequestMapping(value = Array("{configId}/access"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT), modelClass = classOf[Access])
  def getEnvAccess(@PathVariable("projectId") projectId: Int,
                   @PathVariable("configId") configId: Int,
                   request: HttpServletRequest): ItemWrapper[Access] = {
    val (users, groups) = envAuthService.getConfigAccessGrantees(configId)
    Access(Users.of(userService).forUsernames(users).toArray, groups.toArray)
  }

  @RequestMapping(value = Array("{configId}/access"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateEnvAccess(@PathVariable("projectId") projectId: Int,
                      @PathVariable("configId") configId: Int,
                      request: HttpServletRequest): ExtendedResult[_] = {
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

    envAuthService.grantConfigAccess(configId, users.distinct, groups.distinct)

    Success(
      Map(
        "nonExistentUsers" -> nonExistentUsers,
        "nonExistentGroups" -> nonExistentGroups
      )
    )
  }

}
