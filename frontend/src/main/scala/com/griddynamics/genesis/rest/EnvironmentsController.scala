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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
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
import com.griddynamics.genesis.api.EnvironmentDetails

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/envs"))
class EnvironmentsController extends RestApiExceptionsHandler {
  import GenesisRestController._

  @Autowired var genesisService: GenesisService = _

  @Value("${genesis.system.server.mode:frontend}")
  var mode = ""

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def createEnv(@PathVariable projectId: Int, request: HttpServletRequest, response : HttpServletResponse) = {
    val paramsMap = extractParamsMap(request)
    val envName = extractValue("envName", paramsMap)
    val templateName = extractValue("templateName", paramsMap)
    val templateVersion = extractValue("templateVersion", paramsMap)
    val variables = extractVariables(paramsMap)

    val user = mode match {
      case "backend" => request.getHeader(TunnelFilter.SEC_HEADER_NAME)
      case _ => getCurrentUser
    }
    genesisService.createEnv(projectId.toInt, envName, user, templateName, templateVersion, variables)
  }

  private def produceLogs(logs: Seq[String], response: HttpServletResponse) {
    val text = if (logs.isEmpty)
      "No logs yet"
    else
      logs.reduceLeft(_ + "\n" + _)

    response.setContentType("text/plain")
    response.getWriter.write(text)
    response.getWriter.flush()
  }

  @RequestMapping(value=Array("{envName}/logs/{stepId}"))
  def stepLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envName") envName: String,
               @PathVariable stepId: Int,
               response: HttpServletResponse,
               request: HttpServletRequest) {
    assertEnvExist(projectId, envName)

    produceLogs(genesisService.getLogs(envName, stepId), response)
  }

  @RequestMapping(value=Array("{envName}/action_logs/{actionUUID}"))
  def actionLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envName") envName: String,
               @PathVariable actionUUID: String,
               response: HttpServletResponse) {
    assertEnvExist(projectId, envName)

    produceLogs(genesisService.getLogs(envName, actionUUID), response)
  }

  @RequestMapping(value = Array("{envName}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteEnv( @PathVariable("projectId") projectId: Int,
                 @PathVariable("envName") envName: String,
                 request: HttpServletRequest) = {
    assertEnvExist(projectId, envName)
    genesisService.destroyEnv(envName, projectId, Map[String, String]())
  }


  @RequestMapping(value = Array("{envName}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeEnv(@PathVariable("projectId") projectId: Int,
                  @PathVariable("envName") envName: String,
                  response : HttpServletResponse) : EnvironmentDetails = {
    assertEnvExist(projectId, envName)
    genesisService.describeEnv(envName, projectId).getOrElse(throw new ResourceNotFoundException("Environment [" + envName + "] was not found"))
  }


  @RequestMapping(value = Array("{envName}/history"), method = Array(RequestMethod.GET), params = Array("page_offset", "page_length"))
  @ResponseBody
  def workflowsHistory(@PathVariable("projectId") projectId: Int,
                       @PathVariable("envName") envName: String,
                       @RequestParam("page_offset") pageOffset: Int,
                       @RequestParam("page_length") pageLength: Int,
                       response : HttpServletResponse): WorkflowHistory = {
    assertEnvExist(projectId, envName)
    genesisService.workflowHistory(envName, projectId, pageOffset, pageLength).getOrElse(
        throw new ResourceNotFoundException("Environment [" + envName + "] was not found")
    )
  }


  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def listEnvs(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = genesisService.listEnvs(projectId)

  @RequestMapping(method = Array(RequestMethod.GET), params = Array("filter"))
  @ResponseBody
  def listEnvsWithFilter(@PathVariable("projectId") projectId: Int,
                         @RequestParam("filter") filter: String,
                         request: HttpServletRequest) = {
    filter match {
      case EnvFilter(statuses @ _*) => genesisService.listEnvs(projectId, statuses.map(_.toString))
      case _ => throw new InvalidInputException
    }
  }

  @RequestMapping(value = Array("{envName}/actions"), method = Array(RequestMethod.POST))
  @ResponseBody
  def executeAction(@PathVariable("projectId") projectId: Int,
                    @PathVariable("envName") env: String,
                    request: HttpServletRequest) = {
    assertEnvExist(projectId, env)

    val requestMap = extractParamsMap(request)
    extractNotEmptyValue("action", requestMap) match {
      case "cancel" => {
        genesisService.cancelWorkflow(env, projectId)
        RequestResult(isSuccess = true)
      }

      case "execute" => {
        val parameters = extractMapValue("parameters", requestMap)
        val workflow = extractValue("workflow", parameters)
        genesisService.requestWorkflow(env, projectId, workflow, extractVariables(parameters))
      }

      case "resetEnvStatus" => {
        genesisService.resetEnvStatus(env, projectId)
      }

      case _ => throw new InvalidInputException ()
    }
  }

  @RequestMapping(value = Array("{envName}/steps/{stepId}/actions"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getStepActions(@PathVariable("projectId") projectId: Int,
                     @PathVariable("envName") env: String,
                     @PathVariable("stepId") stepId: Int,
                     request: HttpServletRequest): Seq[ActionTracking] = {
    assertEnvExist(projectId, env)
    genesisService.getStepLog(stepId)
  }

  private def assertEnvExist(projectId: Int, env: String) {
    if (!genesisService.isEnvExists(env, projectId)) {
      throw new ResourceNotFoundException("Environment [" + env + "] wasn't found in project [id = "+ projectId + "]")
    }
  }
}
