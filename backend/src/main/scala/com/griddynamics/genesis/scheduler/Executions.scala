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

import org.quartz.{JobDetail, Job, JobDataMap, JobKey, TriggerKey}
import com.griddynamics.genesis.scheduler.jobs.{NotificationJob, DestructionStatusCheckJob, WorkflowExecutionJob}
import java.util.Date
import com.griddynamics.genesis.model.VariablesField

object GroupIdentity {
  def forEnv(projectId: Int, envId: Int) = s"${forProjectPrefix(projectId)}-env-" + envId

  def projectId(key: TriggerKey) =  {
    val pat = s"$basePrefix-group-for-project-(\\d+).*".r
    val pat(projectId) = key.getGroup
    projectId.toInt
  }

  def basePrefix = "execution"
  def forProjectPrefix(projectId: Int) = s"$basePrefix-group-for-project-$projectId"
}

sealed trait ExecutionId {
  def projectId: Int
  def envId: Int
  def id: String

  final def triggerKey: TriggerKey = new TriggerKey(id, GroupIdentity.forEnv(projectId, envId))
  final def jobKey: JobKey = new JobKey(id, GroupIdentity.forEnv(projectId, envId))
}

object Execution {
  def apply(details: JobDetail): Execution = {
    val jobClass = details.getJobClass
    if(jobClass.isAssignableFrom(classOf[WorkflowExecutionJob])) {
      new WorkflowExecution(details.getJobDataMap)
    } else if (jobClass.isAssignableFrom(classOf[NotificationJob])) {
      new ExpireNotification(details.getJobDataMap)
    } else if (jobClass.isAssignableFrom(classOf[DestructionStatusCheckJob])) {
      new DestructionCheck(details.getJobDataMap)
    } else {
      throw new IllegalArgumentException(s"Job class ${details.getJobClass} is not supported")
    }
  }

  def isWorkflowExecution(key: TriggerKey) = {
    key.getName.matches("execution-.+-env-.+")
  }
}

sealed trait Execution extends ExecutionId {
  def projectId: Int
  def toJobDataMap: JobDataMap
  def jobClass: Class[_ <: Job]
}

sealed class WorkflowExecutionId(val projectId: Int, val envId: Int, val workflow: String) extends ExecutionId {
  def id = s"execution-$workflow-env-$envId"
}

object WorkflowExecutionId {


}

case class WorkflowExecution(
    override val workflow: String,
    override val envId: Int,
    override val projectId: Int,
    requestedBy: String,
    variables: Map[String, String])
  extends WorkflowExecutionId(projectId, envId, workflow)
  with Execution {

  def this(jobDataMap: JobDataMap) = this(
    jobDataMap.getString("workflowName"),
    jobDataMap.getInt("envId"),
    jobDataMap.getInt("projectId"),
    jobDataMap.getString("requestedBy"),
    VariablesField.unmarshal(jobDataMap.getString("variables"))
  )

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("workflowName", workflow)
    data.put("projectId", projectId )
    data.put("requestedBy", requestedBy)
    data.put("variables", VariablesField.marshal(variables))
    data
  }

  def jobClass = classOf[WorkflowExecutionJob]
}

//case class Destruction(projectId: Int, envId: Int) extends ExecutionId {
//  val id = "destruction-env-" + envId
//}

sealed class ExpireNotificationId(val projectId: Int, val envId: Int) extends ExecutionId {
  val id = s"env-expire-notification-$envId"
}

case class ExpireNotification(override val envId: Int, override val projectId: Int, destroyDate: Date) extends ExpireNotificationId(projectId, envId) with Execution {

  def this(jobDataMap: JobDataMap) = this(jobDataMap.getInt("envId"), jobDataMap.getInt("projectId"), new Date(jobDataMap.getLong("destroyDate")))

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("projectId", projectId )
    data.put("destroyDate", destroyDate.getTime )
    data
  }

  def jobClass = classOf[NotificationJob]
}

sealed class DestructionCheckId(val projectId: Int, val envId: Int) extends ExecutionId {
  def id = s"env-destruction-check-$envId"
}

case class DestructionCheck(override val envId: Int, override val projectId: Int, destructionTrigger: String)
  extends DestructionCheckId(projectId, envId) with Execution {

  def this(jobDataMap: JobDataMap) = this(jobDataMap.getInt("envId"), jobDataMap.getInt("projectId"), jobDataMap.getString("destructionTriggerName"))

  def toJobDataMap = {
    val data = new JobDataMap()
    data.put("envId", envId)
    data.put("projectId", projectId )
    data.put("destructionTriggerName", destructionTrigger)
    data
  }

  def destructionTriggerKey = new TriggerKey(destructionTrigger, GroupIdentity.forEnv(projectId, envId))

  def jobClass = classOf[DestructionStatusCheckJob]
}
