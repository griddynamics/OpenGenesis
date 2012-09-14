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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.rest

import filters.EnvFilter
import org.springframework.stereotype.Controller
import scala.Array
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.http.TunnelFilter
import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service.EnvironmentAccessService
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.ActionTracking
import com.griddynamics.genesis.api.EnvironmentDetails
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.api.WorkflowHistory
import org.springframework.security.access.prepost.PostFilter
import org.springframework.http.{HttpHeaders, MediaType}

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/envs"))
class EnvironmentsController extends RestApiExceptionsHandler {
  import GenesisRestController._

  @Autowired var genesisService: GenesisService = _

  @Autowired var envAuthService: EnvironmentAccessService = _

  @Value("${genesis.system.server.mode:frontend}")
  var mode = ""

  @RequestMapping(value=Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def createEnv(@PathVariable projectId: Int, request: HttpServletRequest, response : HttpServletResponse) = {
    val paramsMap = extractParamsMap(request)
    val envName = extractValue("envName", paramsMap).trim
    val templateName = extractValue("templateName", paramsMap)
    val templateVersion = extractValue("templateVersion", paramsMap)
    val variables = extractVariables(paramsMap)

    val user = mode match {
      case "backend" => request.getHeader(TunnelFilter.SEC_HEADER_NAME)
      case _ => getCurrentUser
    }
    genesisService.createEnv(projectId, envName, user, templateName, templateVersion, variables)
  }

  import MediaType.{TEXT_PLAIN, TEXT_PLAIN_VALUE, TEXT_HTML_VALUE}
  private def produceLogs(logs: Seq[String], response: HttpServletResponse, headers: HttpHeaders) {
    val text = if (logs.isEmpty)
      "No logs yet"
    else
      logs.reduceLeft(_ + "\n" + _)

    val plainText = headers.getAccept.contains(TEXT_PLAIN)
    response.setContentType(if (plainText) TEXT_PLAIN_VALUE else TEXT_HTML_VALUE)
    response.getWriter.write(if (plainText) text else "<pre>%s</pre>".format(text))
    response.getWriter.flush()
  }

  @RequestMapping(value=Array("{envId}/logs/{stepId}"), produces = Array(TEXT_PLAIN_VALUE, TEXT_HTML_VALUE))
  def stepLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envId") envId: Int,
               @PathVariable stepId: Int,
               @RequestParam(value = "include_actions", required = false, defaultValue = "false") includeActions: Boolean,
               @RequestHeader headers: HttpHeaders,
               response: HttpServletResponse) {
    validateStepId(stepId, envId)
    produceLogs(genesisService.getLogs(envId, stepId, includeActions), response, headers)
  }

  @RequestMapping(value=Array("{envName}/action_logs/{actionUUID}"), produces = Array(TEXT_PLAIN_VALUE, TEXT_HTML_VALUE))
  def actionLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envName") envId: Int,
               @PathVariable actionUUID: String,
               @RequestHeader headers: HttpHeaders,
               response: HttpServletResponse) {
    produceLogs(genesisService.getLogs(envId, actionUUID), response, headers)
  }

  @RequestMapping(value = Array("{envName}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteEnv( @PathVariable("projectId") projectId: Int,
                 @PathVariable("envName") envId: Int,
                 request: HttpServletRequest) = {
    genesisService.destroyEnv(envId, projectId, Map[String, String](), getCurrentUser)
  }


  @RequestMapping(value = Array("{envName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeEnv(@PathVariable("projectId") projectId: Int,
                  @PathVariable("envName") envId: Int,
                  response : HttpServletResponse) : EnvironmentDetails = {
    genesisService.describeEnv(envId, projectId).getOrElse(throw new ResourceNotFoundException("Environment [" + envId + "] was not found"))
  }


  @RequestMapping(value = Array("{envId}/history"), method = Array(RequestMethod.GET), params = Array("page_offset", "page_length"))
  @ResponseBody
  def workflowsHistory(@PathVariable("projectId") projectId: Int,
                       @PathVariable("envId") envId: Int,
                       @RequestParam("page_offset") pageOffset: Int,
                       @RequestParam("page_length") pageLength: Int,
                       response : HttpServletResponse): WorkflowHistory = {
    genesisService.workflowHistory(envId, projectId, pageOffset, pageLength).getOrElse(
        throw new ResourceNotFoundException("Environment [" + envId + "] was not found")
    )
  }


  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @PostFilter("not(@environmentSecurity.restrictionsEnabled()) " +
    "or hasRole('ROLE_GENESIS_ADMIN')" +
    "or hasPermission( #projectId, 'com.griddynamics.genesis.api.Project', 'administration') " +
    "or hasPermission(filterObject, 'read')")
  def listEnvs(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = genesisService.listEnvs(projectId)

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET), params = Array("filter"))
  @ResponseBody
  @PostFilter("not(@environmentSecurity.restrictionsEnabled()) " +
    "or hasRole('ROLE_GENESIS_ADMIN')" +
    "or hasPermission( #projectId, 'com.griddynamics.genesis.api.Project', 'administration') " +
    "or hasPermission(filterObject, 'read')")
  def listEnvsWithFilter(@PathVariable("projectId") projectId: Int,
                         @RequestParam("filter") filter: String,
                         request: HttpServletRequest) = {
    filter match {
      case EnvFilter(statuses @ _*) => genesisService.listEnvs(projectId, statuses.map(_.toString))
      case _ => throw new InvalidInputException
    }
  }

  @RequestMapping(value = Array("{envId}/actions"), method = Array(RequestMethod.POST))
  @ResponseBody
  def executeAction(@PathVariable("projectId") projectId: Int,
                    @PathVariable("envId") envId: Int,
                    request: HttpServletRequest) = {

    val requestMap = extractParamsMap(request)
    extractNotEmptyValue("action", requestMap) match {
      case "cancel" => {
        genesisService.cancelWorkflow(envId, projectId)
        RequestResult(isSuccess = true)
      }

      case "execute" => {
        val parameters = extractMapValue("parameters", requestMap)
        val workflow = extractValue("workflow", parameters)
        genesisService.requestWorkflow(envId, projectId, workflow, extractVariables(parameters), getCurrentUser)
      }

      case "resetEnvStatus" => {
        genesisService.resetEnvStatus(envId, projectId)
      }

      case _ => throw new InvalidInputException ()
    }
  }

  @RequestMapping(value = Array("{envId}/steps/{stepId}/actions"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getStepActions(@PathVariable("projectId") projectId: Int,
                     @PathVariable("envId") envId: Int,
                     @PathVariable("stepId") stepId: Int,
                     request: HttpServletRequest): Seq[ActionTracking] = {
    validateStepId(stepId, envId)
    genesisService.getStepLog(stepId)
  }

  @RequestMapping(value = Array("{envId}/access"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getEnvAccess(@PathVariable("projectId") projectId: Int,
                   @PathVariable("envId") envId: Int,
                   request: HttpServletRequest) = {
    val (users, groups) = envAuthService.getAccessGrantees(envId)
    Map("users" -> users, "groups" -> groups)
  }

  @RequestMapping(value = Array("{envId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateEnv(@PathVariable("projectId") projectId: Int, @PathVariable("envId") envId: Int, request: HttpServletRequest) = {
      val paramsMap = GenesisRestController.extractParamsMap(request)
      val env = GenesisRestController.extractMapValue("environment", paramsMap)
      val envName = env.getOrElse("name", throw new MissingParameterException("environment.name"))
      genesisService.updateEnvironmentName(envId, projectId, envName.asInstanceOf[String].trim)
  }

  @RequestMapping(value = Array("{envId}/access"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateEnvAccess(@PathVariable("projectId") projectId: Int,
                      @PathVariable("envId") envId: Int,
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

    envAuthService.grantAccess(envId, users.distinct, groups.distinct )
    Success(None)
  }

  private def validateStepId(stepId: Int, envId: Int) {
    if (!genesisService.stepExists(stepId, envId)) throw new ResourceNotFoundException("Step [id=%d] wasn't found in environment [id=%d]"
      .format(stepId, envId))
  }
}
