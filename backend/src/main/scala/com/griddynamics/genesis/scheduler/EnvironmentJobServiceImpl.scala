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
 */ package com.griddynamics.genesis.scheduler

import com.griddynamics.genesis.model.Environment
import com.griddynamics.genesis.service.{TemplateDefinition, EnvironmentConfigurationService, TemplateService, StoreService}
import java.util.Date
import java.util.concurrent.TimeUnit
import org.springframework.transaction.annotation.Transactional
import scala.concurrent.duration._
import com.griddynamics.genesis.api.{Configuration, ScheduledJobDetails, Success, ExtendedResult}
import com.griddynamics.genesis.bean.RequestBrokerImpl
import com.griddynamics.genesis.util.Logging

trait EnvironmentJobService {
  def scheduleDestruction(projectId: Int, envId: Int, date: Date, requestedBy: String)
  def destructionDate(env: Environment): Option[Date]
  def executionDate(env: Environment, workflow: String): Option[Date]
  def removeScheduledDestruction(projectId: Int, envId: Int)
  def removeAllScheduledJobs(projectId: Int, envId: Int)
  def listScheduledJobs(projectId: Int, envId: Int): Seq[ScheduledJobDetails]
  def removeJob(projectId: Int, envId: Int, jobId: String)
  def scheduleExecution(projectId: Int, envId: Int, workflow: String, parameters: Map[String, String], date: Date, requestedBy: String): ExtendedResult[Date]
}

class EnvironmentJobServiceImpl(scheduler: SchedulingService,
                            storeService: StoreService,
                            templateService: TemplateService,
                            configurationService: EnvironmentConfigurationService) extends EnvironmentJobService with Logging {

  @Transactional
  def scheduleDestruction(projectId: Int, envId: Int, date: Date, requestedBy: String) = {
    val env = storeService.findEnv(envId, projectId).getOrElse {
      throw new IllegalArgumentException(s"Couldn't find env id = $envId")
    }
    val template = templateService.findTemplate(env).getOrElse {
      throw new IllegalArgumentException("Couldn't find template")
    }

    scheduleDestructionJobs(env, template, requestedBy, date)
  }


  private def scheduleDestructionJobs(env: Environment, template: TemplateDefinition, requestedBy: String, date: Date) = {
    val daysFromNow: Long = Duration(date.getTime - System.currentTimeMillis(), MILLISECONDS).toDays
    if (executionDate(env, template.destroyWorkflow.name).isDefined) {
      this.removeScheduledDestruction(env.projectId, env.id)
    }

    val destruction = new WorkflowExecution(template.destroyWorkflow.name, env.id, env.projectId, requestedBy, Map())
    scheduler.schedule(destruction, date)

    val notificationDate = daysFromNow match {
      case d if d > 2 => Some(new Date(date.getTime - TimeUnit.DAYS.toMillis(1)))
      case d if d > 1 => Some(new Date(date.getTime - TimeUnit.HOURS.toMillis(12)))
      case _ => None
    }

    notificationDate.foreach {
      d =>
        val notification = new ExpireNotification(env.id, env.projectId, date)
        scheduler.schedule(notification, d)
    }

    val check = new DestructionCheck(env.id, env.projectId, destruction.triggerKey.getName)
    scheduler.schedule(check, new Date(date.getTime + TimeUnit.MINUTES.toMillis(5)))
  }

  @Transactional(readOnly = true)
  def executionDate(env: Environment, workflow: String): Option[Date] = {
    val id = new WorkflowExecutionId(env.id, workflow)
    scheduler.getScheduledDate(id)
  }

  @Transactional(readOnly = true)
  def destructionDate(env: Environment) = {
    templateService.findTemplate(env).flatMap ( t => executionDate(env, t.destroyWorkflow.name) )
  }

  @Transactional
  def removeAllScheduledJobs(projectId: Int, envId: Int) {
    val jobs = listScheduledJobs(projectId, envId)
    jobs.foreach { j => removeJob(projectId, envId, j.id) }
  }

  @Transactional
  def removeScheduledDestruction(projectId: Int, envId: Int) {
    for ( env <- storeService.findEnv(envId, projectId);
          template <- templateService.findTemplate(env) ) {
      val jobs = Seq(new DestructionCheckId(envId), new ExpireNotificationId(envId), new WorkflowExecutionId(envId, template.destroyWorkflow.name))
      scheduler.removeScheduledJobs(jobs)
    }
  }


  @Transactional
  def scheduleExecution(projectId: Int, envId: Int, workflow: String, parameters: Map[String, String], date: Date, requestedBy: String): ExtendedResult[Date] = {
    val env = storeService.findEnv(envId).getOrElse {
      throw new IllegalArgumentException(s"Couldn't find env id = $envId")
    }
    val template = templateService.findTemplate(env).getOrElse(
      throw new IllegalArgumentException(s"Couldn't find template for environment ${env.id}")
    )
    val configuration = configurationService.get(env.projectId, env.configurationId).getOrElse(
      throw new IllegalArgumentException(s"Couldn't find environment configuration id = ${env.configurationId} in project ${env.projectId}")
    )
    if(template.destroyWorkflow.name == workflow) {
      scheduleDestructionJobs(env, template, requestedBy, date)
      Success(date)
    } else {
      scheduler.removeScheduledJob(new WorkflowExecutionId(envId, workflow))
      scheduleWorkflow(template, workflow, parameters, configuration, env, requestedBy, date)
    }
  }


  private def scheduleWorkflow(template: TemplateDefinition,
                               workflow: String,
                               parameters: Map[String, String],
                               configuration: Configuration,
                               env: Environment,
                               requestedBy: String,
                               date: Date): ExtendedResult[Date] = {
    template.getValidWorkflow(workflow).flatMap { wf =>
      RequestBrokerImpl.validateWorkflow(wf, parameters, configuration).map { _ =>
        val execution = new WorkflowExecution(workflow, env.id, env.projectId, requestedBy, parameters)
        scheduler.schedule(execution, date)
        date
      }
    }
  }

  @Transactional
  def removeJob(projectId: Int, envId: Int, jobId: String) {
    val execution = scheduler.getJob(envId, jobId)
    execution match {
      case e: WorkflowExecution =>
        if(isDestroyWorkflow(envId, projectId, e.workflow)) {
          removeScheduledDestruction(projectId, envId)
        } else {
          scheduler.removeScheduledJob(new WorkflowExecutionId(envId, e.workflow))
        }
      case _ => //do nothing cause only workflow executions are propagated to upper layers
    }
  }



  @Transactional
  def listScheduledJobs(projectId: Int, envId: Int): Seq[ScheduledJobDetails] = {
    val env = storeService.findEnv(envId).getOrElse {
      throw new IllegalArgumentException(s"Couldn't find env id = $envId")
    }

    val s = scheduler.listJobs(projectId, envId)
    val details = s.collect { case (date, execution: WorkflowExecution) if date != null => new ScheduledJobDetails(
      id = execution.id,
      projectId = projectId,
      envId = env.id,
      date = date.getTime,
      workflow = execution.workflow,
      variables = execution.variables,
      scheduledBy = execution.requestedBy,
      failureDescription = None
    )}
    details
  }


  private def isDestroyWorkflow(envId: Int, projectId: Int, workflowName: String): Boolean = {
    val definition = for {
      env <- storeService.findEnv(envId, projectId)
      template <- templateService.findTemplate(env)}
    yield template.destroyWorkflow

    definition.map(_.name == workflowName).getOrElse(false)
  }

}
