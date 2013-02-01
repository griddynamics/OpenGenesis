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

import annotations.LinkTarget._
import filters.EnvFilter
import links._
import HrefBuilder._
import links.ItemWrapper
import links.WebPath
import CollectionWrapper._
import org.springframework.stereotype.Controller
import scala.Array
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.http.TunnelFilter
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.griddynamics.genesis.service.EnvironmentAccessService
import org.springframework.security.access.prepost.PostFilter
import org.springframework.http.{HttpHeaders, MediaType}
import java.util.{Date, TimeZone, Locale}
import org.springframework.security.access.AccessDeniedException
import com.griddynamics.genesis.repository.ConfigurationRepository
import org.apache.commons.lang3.StringEscapeUtils
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.template.dsl.groovy.Reserved

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/envs"))
class EnvironmentsController extends RestApiExceptionsHandler {
  import GenesisRestController._

  @Autowired var genesisService: GenesisService = _

  @Autowired var envAuthService: EnvironmentAccessService = _
  @Autowired var configRepository: ConfigurationRepository = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _


  @Value("${genesis.system.server.mode:frontend}")
  var mode = ""

  @RequestMapping(value=Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def createEnv(@PathVariable("projectId") projectId: Int, request: HttpServletRequest, response : HttpServletResponse): ExtendedResult[Int] = {
    val paramsMap = extractParamsMap(request)
    val envName = extractValue("envName", paramsMap).trim
    val templateName = extractValue("templateName", paramsMap)
    val templateVersion = extractValue("templateVersion", paramsMap)
    val variables = extractVariables(paramsMap)
    val config =  variables.get(Reserved.configRef).map { cid =>
      configRepository.get(projectId, cid.toInt).getOrElse(throw new ResourceNotFoundException("Failed to find config with id = %s in project %d".format(cid, projectId)))
    }.getOrElse {
      configRepository.getDefaultConfig(projectId) match {
        case Success(s) => s
        case f: Failure => return f
      }
    }
    val timeToLive = try {
      extractOption("timeToLive", paramsMap).map(min => TimeUnit.MINUTES.toMillis(min.toLong))
    } catch {
      case e: NumberFormatException => return Failure(serviceErrors = Map("timeToLive" -> "Should be numeric"))
    }

    if(!envAuthService.hasAccessToConfig(projectId, config.id.get, getCurrentUser, getCurrentUserAuthorities)) {
      throw new AccessDeniedException("User doesn't have access to configuration id=%s".format(config))
    }

    val user = mode match {
      case "backend" => request.getHeader(TunnelFilter.SEC_HEADER_NAME)
      case _ => getCurrentUser
    }
    genesisService.createEnv(projectId, envName, user, templateName, templateVersion, variables, config, timeToLive)
  }

  private val LINK_REGEX = """(?i)\[link:(.*?)(?:\|([^\[\]]+))?\]""".r

  import MediaType.{TEXT_PLAIN, TEXT_PLAIN_VALUE, TEXT_HTML, TEXT_HTML_VALUE}
  private def produceLogs(logs: Seq[StepLogEntry], response: HttpServletResponse,
                          headers: HttpHeaders, locale: Locale, timeZone: TimeZone) {
    import scala.collection.JavaConversions._
    val supportHtml =
      headers.getAccept.toSet.exists(_.isCompatibleWith(TEXT_HTML))

    response.setContentType(if (supportHtml) TEXT_HTML_VALUE else TEXT_PLAIN_VALUE)

    val writer = response.getWriter

    if (supportHtml) writer.print("<pre>")

    def escape(str: String) = str.replace("\\", "\\\\").replace("$", "\\$")

    if (logs.isEmpty)
      writer.write("No logs yet")
    else
      logs.foreach { entry =>
        var record = entry.toString(locale, timeZone) + "\n"
        if (supportHtml) record =
          LINK_REGEX.replaceAllIn(StringEscapeUtils.escapeHtml4(record), m =>
            <a target="_blank" href={ escape(m.group(1)) }>{ escape(if (m.group(2) == null) m.group(1) else m.group(2)) }</a>.toString())
            .replaceAll("""(?i)\[(link)\\:(.*?)\]""", "[$1:$2]")
        writer.write(record)
      }

    if (supportHtml) writer.print("</pre>")

    response.getWriter.flush()
  }

  private def getTimezoneByOffset(timezoneOffset: java.lang.Integer): TimeZone =
    Option(timezoneOffset).map { offset =>
      TimeZone.getTimeZone("GMT%0+3d:%02d".format(-offset / 60, java.lang.Math.abs(offset % 60)))
    }.getOrElse(TimeZone.getDefault)

  @RequestMapping(value=Array("{envId}/logs/{stepId}"), produces = Array(TEXT_PLAIN_VALUE, TEXT_HTML_VALUE))
  def stepLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envId") envId: Int,
               @PathVariable("stepId") stepId: Int,
               @RequestParam(value = "include_actions", required = false, defaultValue = "false") includeActions: Boolean,
               @RequestParam(value = "timezone_offset", required = false) timezoneOffset: java.lang.Integer,
               @RequestHeader headers: HttpHeaders,
               response: HttpServletResponse,
               locale: Locale) {
    validateStepId(stepId, envId)
    produceLogs(genesisService.getLogs(envId, stepId, includeActions), response, headers, locale, getTimezoneByOffset(timezoneOffset))
  }

  @RequestMapping(value=Array("{envName}/action_logs/{actionUUID}"), produces = Array(TEXT_PLAIN_VALUE, TEXT_HTML_VALUE))
  def actionLogs(@PathVariable("projectId") projectId: Int,
               @PathVariable("envName") envId: Int,
               @PathVariable("actionUUID") actionUUID: String,
               @RequestParam(value = "timezone_offset", required = false) timezoneOffset: java.lang.Integer,
               @RequestHeader headers: HttpHeaders,
               response: HttpServletResponse,
               locale: Locale) {
    produceLogs(genesisService.getLogs(envId, actionUUID), response, headers, locale, getTimezoneByOffset(timezoneOffset))
  }

  @RequestMapping(value = Array("{envName}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteEnv( @PathVariable("projectId") projectId: Int,
                 @PathVariable("envName") envId: Int,
                 request: HttpServletRequest) = {
    genesisService.destroyEnv(envId, projectId, Map[String, String](), getCurrentUser)
  }


  @RequestMapping(value = Array("{envId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def describeEnv(@PathVariable("projectId") projectId: Int,
                  @PathVariable("envId") envId: Int, request: HttpServletRequest) : ItemWrapper[EnvironmentDetails] = {
    val wrapper: ItemWrapper[EnvironmentDetails] = genesisService.describeEnv(envId, projectId).
      getOrElse(throw new ResourceNotFoundException("Environment [" + envId + "] was not found"))
    implicit val req: HttpServletRequest = request
    val top = WebPath(request)
    wrapper.withLinksToSelf(GET, PUT, DELETE).withLinks(
      LinkBuilder(top / "history", COLLECTION, classOf[WorkflowHistory], GET),
      LinkBuilder(top / "actions", COLLECTION, POST) // TODO: GET not present now. User can only post there.
    ).filtered()
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
    "or hasRole('ROLE_GENESIS_ADMIN') or hasRole('ROLE_GENESIS_READONLY')" +
    "or hasPermission( #projectId, 'com.griddynamics.genesis.api.Project', 'administration') " +
    "or hasPermission(filterObject, 'read')")
  def listEnvsWithFilter(@PathVariable("projectId") projectId: Int,
                         @RequestParam(value="filter", required = false, defaultValue = "") filter: String,
                         @RequestParam(value="sorting", required = false, defaultValue = "name") ordering: Ordering,
                         request: HttpServletRequest) = {

    val environments: Seq[Environment] = filter match {
      case EnvFilter(statuses@_*) => genesisService.listEnvs(projectId, Option(statuses.map(_.toString)), Option(ordering))
      case "" => genesisService.listEnvs(projectId, ordering = Option(ordering))
      case _ => throw new InvalidInputException
    }
    implicit val req: HttpServletRequest = request
    val wrapped = environments.map(environment =>
      wrap(environment).withLinks(LinkBuilder(HrefBuilder.withPathParam(request, environment.id),
        SELF, classOf[Environment], GET)).filtered()
    )

    wrapCollection(wrapped).withLinks(LinkBuilder(request,
      SELF, classOf[Environment], POST)).filtered()
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
        Success(envId)
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

  @RequestMapping(value = Array("{envId}/actions/{workflow}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getWorkflow(@PathVariable("projectId") projectId: Int,
                  @PathVariable("envId") envId: Int,
                  @PathVariable("workflow") workflowName: String) = try {
    genesisService.getWorkflow(projectId, envId, workflowName)
  } catch {
    case e: Exception =>
      log.error(e, s"Failed to get workflow ${workflowName} for env id=${envId}, project id=${envId}")
      Failure(compoundServiceErrors = List(e.getMessage), stackTrace = Option(e.getStackTraceString))
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

  @RequestMapping(value = Array("{envId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateEnv(@PathVariable("projectId") projectId: Int, @PathVariable("envId") envId: Int, request: HttpServletRequest) = {
      val paramsMap = GenesisRestController.extractParamsMap(request)
      val env = GenesisRestController.extractMapValue("environment", paramsMap)
      val envName = env.getOrElse("name", throw new MissingParameterException("environment.name"))
      genesisService.updateEnvironmentName(envId, projectId, envName.asInstanceOf[String].trim)
  }

  private def validateStepId(stepId: Int, envId: Int) {
    if (!genesisService.stepExists(stepId, envId)) throw new ResourceNotFoundException("Step [id=%d] wasn't found in environment [id=%d]"
      .format(stepId, envId))
  }

  @RequestMapping(value = Array("{envId}/timeToLive"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def expandLiveTime(@PathVariable("projectId")projectId: Int, @PathVariable("envId") envId: Int, request: HttpServletRequest): ExtendedResult[Date] = {
    val paramsMap = GenesisRestController.extractParamsMap(request)
    try {
      val timeToLive = TimeUnit.MINUTES.toMillis(extractValue("value", paramsMap).toLong)
      genesisService.updateTimeToLive(projectId, envId, timeToLive)
    } catch {
      case e: NumberFormatException => throw new InvalidInputException("Value should be numeric")
    }
  }

  @RequestMapping(value = Array("{envId}/timeToLive"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def removeLiveTime(@PathVariable("projectId")projectId: Int, @PathVariable("envId") envId: Int, request: HttpServletRequest) = {
    genesisService.removeTimeToLive(projectId, envId)
  }

}
