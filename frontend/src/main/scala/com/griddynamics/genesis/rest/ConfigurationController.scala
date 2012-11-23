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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.repository.{Direction, ConfigurationOrdering, ConfigurationRepository}
import scala.Array
import javax.validation.Valid
import com.griddynamics.genesis.api.ExtendedResult
import org.springframework.security.access.prepost.PostFilter
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.Configuration
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.api.Failure
import scala.Some
import com.griddynamics.genesis.service.{StoreService, EnvironmentAccessService}

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/configs"))
class ConfigurationController extends RestApiExceptionsHandler{

  @Autowired var configRepository: ConfigurationRepository = _
  @Autowired var envAuthService: EnvironmentAccessService = _
  @Autowired var storeService: StoreService = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @PostFilter("not(@environmentSecurity.restrictionsEnabled()) " +
    "or hasRole('ROLE_GENESIS_ADMIN') or hasRole('ROLE_GENESIS_READONLY')" +
    "or hasPermission( #projectId, 'com.griddynamics.genesis.api.Project', 'administration') " +
    "or hasPermission(filterObject, 'read')")
  def list(@PathVariable("projectId") projectId: Int,
           @RequestParam(value = "sorting", required = false, defaultValue = "name") sorting: String) =
    configRepository.list(projectId, sorting match {
      case "name" => ConfigurationOrdering.byName(Direction.ASC)
      case "~name" => ConfigurationOrdering.byName(Direction.DESC)
      case o => throw new IllegalArgumentException("Unknwon sorting value '%s'".format(o))
    })

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def get(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int) =
    configRepository.get(projectId, id).getOrElse(throw new ResourceNotFoundException("Can not find environment configuration id = %d".format(id)))

  @ResponseBody
  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  def create(@PathVariable("projectId") projectId: Int, @Valid @RequestBody config: Configuration) =
    valid(config.copy(id = None, projectId = projectId)).map(configRepository.save(_))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, @Valid @RequestBody config: Configuration) =
    valid(config.copy(id = Some(id), projectId = projectId)).map { c =>
      configRepository.save(c)
    }

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


  private def valid(config: Configuration): ExtendedResult[Configuration] = {
    val exist = configRepository.findByName(config.projectId, config.name)
    exist match {
      case None => Success(config)
      case Some(c) if c.id.isDefined && config.id == c.id => Success(config)
      case _ => Failure(compoundServiceErrors = Seq("Environment configuration with name %s already exists in project".format(config.name)))
    }
  }

  @RequestMapping(value = Array("{configId}/access"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getEnvAccess(@PathVariable("projectId") projectId: Int,
                   @PathVariable("configId") configId: Int,
                   request: HttpServletRequest) = {
    val (users, groups) = envAuthService.getConfigAccessGrantees(configId)
    Map("users" -> users, "groups" -> groups)
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

    envAuthService.grantConfigAccess(configId, users.distinct, groups.distinct )
    Success(None)
  }

}
