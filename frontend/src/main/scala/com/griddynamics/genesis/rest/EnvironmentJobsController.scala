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

import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.EnvironmentConfigurationService
import com.griddynamics.genesis.scheduler.EnvironmentJobService
import scala.Array
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.rest.GenesisRestController._
import java.util.Date
import com.griddynamics.genesis.rest.links.CollectionWrapper
import com.griddynamics.genesis.api.{Failure, Success, ExtendedResult, ScheduledJobDetails}
import java.util.concurrent.TimeUnit

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/envs"))
class EnvironmentJobsController {
  @Autowired var envConfigService: EnvironmentConfigurationService = _
  @Autowired var schedulingService: EnvironmentJobService = _

  @RequestMapping(value = Array("{envId}/jobs"), method = Array(RequestMethod.POST))
  @ResponseBody
  def scheduleWorkflow(@PathVariable("projectId") projectId: Int,
                       @PathVariable("envId") envId: Int,
                       request: HttpServletRequest) = {
    val requestMap = extractParamsMap(request)
    val parameters = extractMapValue("parameters", requestMap).mapValues(_.toString)
    val workflowName = extractValue("workflow", requestMap)
    try {
      val time = new Date(extractValue("executionDate", requestMap).toLong)
      if(time.before(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)))) {
        Failure(variablesErrors = Map("executionDate" -> "Execution date can't be in the past "))
      } else {
        schedulingService.scheduleExecution(projectId, envId, workflowName, parameters, time, getCurrentUser)
      }
    } catch {
      case e: NumberFormatException => Failure(variablesErrors = Map("executionDate" -> "Invalid timestamp format"))
    }
  }

  @RequestMapping(value = Array("{envId}/jobs"), method = Array(RequestMethod.GET))
  @ResponseBody
  def scheduledJobs(@PathVariable("projectId") projectId: Int,
                    @PathVariable("envId") envId: Int,
                    request: HttpServletRequest): CollectionWrapper[ScheduledJobDetails] = {
    schedulingService.listScheduledJobs(projectId, envId)
  }

  @RequestMapping(value = Array("{envId}/jobs/{jobId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def removeJob(@PathVariable("projectId") projectId: Int,
                @PathVariable("envId") envId: Int,
                @PathVariable("jobId") jobId: String,
                request: HttpServletRequest): ExtendedResult[_] = {
    try {
      schedulingService.removeJob(projectId, envId, jobId)
      Success(jobId)
    } catch {
      case e: Exception => Failure(compoundServiceErrors = Seq(e.getMessage))
    }
  }
}
