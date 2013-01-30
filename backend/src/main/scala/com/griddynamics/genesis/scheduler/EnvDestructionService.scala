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
import com.griddynamics.genesis.service.{TemplateService, StoreService}
import java.util.Date
import java.util.concurrent.TimeUnit
import org.springframework.transaction.annotation.Transactional
import scala.concurrent.duration._

trait DestructionService {
  def scheduleDestruction(projectId: Int, envId: Int, date: Date)
  def destructionDate(env: Environment): Option[Date]
  def destructionDate(env: Environment, destroyWorfklow: String): Option[Date]
  def removeScheduledDestruction(projectId: Int, envId: Int)
}

class EnvDestructionService(scheduler: SchedulingService,
                            storeService: StoreService,
                            templateService: TemplateService) extends DestructionService {

  @Transactional
  def scheduleDestruction(projectId: Int, envId: Int, date: Date) {
    val daysFromNow: Long = Duration(date.getTime - System.currentTimeMillis(), MILLISECONDS).toDays

    for {
      env <- storeService.findEnv(envId, projectId )
      template <- templateService.findTemplate(env)
    } yield {

      if (destructionDate(env, template.destroyWorkflow.name).isDefined) {
        this.removeScheduledDestruction(projectId, envId)
      }

      val destruction = new WorkflowExecution(template.destroyWorkflow.name, envId, projectId)
      scheduler.schedule(destruction, date)

      val notificationDate = daysFromNow match {
        case d if d > 2 => Some(new Date(date.getTime - TimeUnit.DAYS.toMillis(1)))
        case d if d > 1 => Some(new Date(date.getTime - TimeUnit.HOURS.toMillis(12)))
        case _ => None
      }

      notificationDate.foreach { d =>
        val notification = new ExpireNotification(envId, projectId, date)
        scheduler.schedule(notification, d)
      }

      val check = new DestructionCheck(envId, projectId, destruction.triggerKey.getName)
      scheduler.schedule(check, new Date(date.getTime + TimeUnit.MINUTES.toMillis(5)))
    }
  }

  @Transactional(readOnly = true)
  def destructionDate(env: Environment, destroyWorfklow: String): Option[Date] = {
    val execution = new WorkflowExecution(destroyWorfklow, env.id, env.projectId)
    scheduler.getScheduledDate(execution)
  }


  @Transactional(readOnly = true)
  def destructionDate(env: Environment) = {
    templateService.findTemplate(env).flatMap ( t => destructionDate(env, t.destroyWorkflow.name) )
  }

  @Transactional
  def removeScheduledDestruction(projectId: Int, envId: Int) {
    scheduler.removeAllScheduledJobs(projectId, envId)
  }
}
