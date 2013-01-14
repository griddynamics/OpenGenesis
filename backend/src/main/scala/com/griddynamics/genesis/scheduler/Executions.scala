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

import org.quartz.{Job, JobDataMap, JobKey, TriggerKey}
import com.griddynamics.genesis.scheduler.jobs.{NotificationJob, DestructionStatusCheckJob, WorkflowExecutionJob}
import java.util.Date

object GroupIdentity {
  def forEnv(envId: Int) = "execution-group-for-env-" + envId
}

sealed trait ExecutionId {
  def projectId: Int
  def envId: Int
  def id: String

  final def triggerKey: TriggerKey = new TriggerKey(id, GroupIdentity.forEnv(envId))
  final def jobKey: JobKey = new JobKey(id, GroupIdentity.forEnv(envId))
}

sealed trait Execution extends ExecutionId {
  def toJobDataMap: JobDataMap
  def jobClass: Class[_ <: Job]
}

case class WorkflowExecution(workflow: String, envId: Int, projectId: Int) extends Execution {
  val id = "execution-" + workflow +"-env-" + envId

  def this(jobDataMap: JobDataMap) = this(jobDataMap.getString("workflowName"), jobDataMap.getInt("envId"), jobDataMap.getInt("projectId"))

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("workflowName", workflow)
    data.put("projectId", projectId )
    data
  }

  def jobClass = classOf[WorkflowExecutionJob]
}

//case class Destruction(projectId: Int, envId: Int) extends ExecutionId {
//  val id = "destruction-env-" + envId
//}

case class ExpireNotification(envId: Int, projectId: Int, destroyDate: Date) extends Execution {
  def this(jobDataMap: JobDataMap) = this(jobDataMap.getInt("envId"), jobDataMap.getInt("projectId"), new Date(jobDataMap.getLong("destroyDate")))

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("projectId", projectId )
    data.put("destroyDate", destroyDate.getTime )
    data
  }

  val id = "env-expire-notification-" + envId

  def jobClass = classOf[NotificationJob]
}

case class DestructionCheck(envId: Int, projectId: Int, destructionTrigger: String) extends Execution {
  def this(jobDataMap: JobDataMap) = this(jobDataMap.getInt("envId"), jobDataMap.getInt("projectId"), jobDataMap.getString("destructionTriggerName"))

  val id = "env-destruction-check" + envId

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("projectId", projectId )
    data.put("destructionTriggerName", destructionTrigger)
    data
  }

  def destructionTriggerKey = new TriggerKey(destructionTrigger, GroupIdentity.forEnv(envId))

  def jobClass = classOf[DestructionStatusCheckJob]
}
