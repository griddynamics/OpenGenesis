package com.griddynamics.genesis.scheduler.jobs

import org.quartz._
import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.model.{Environment, EnvStatus}
import java.util.Date
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.api.{Failure, ExtendedResult}
import com.griddynamics.genesis.scheduler.{NotificationService, WorkflowExecution}
import com.griddynamics.genesis.util.Logging

class WorkflowExecutionJob(
    broker: RequestBroker,
    storeService: StoreService,
    notificationService: NotificationService)
  extends Job
  with Logging {

  def execute(context: JobExecutionContext) {
    val details = new WorkflowExecution(context.getMergedJobDataMap)
    val env = storeService.findEnv(details.envId, details.projectId).getOrElse{
      throw new JobExecutionException(s"Scheduling job ${details.toString} failed to perform execution because env wasn't found in database")
    }
    env.status match {
      case EnvStatus.Destroyed => /* do nothing */
      case EnvStatus.Busy => rescheduleJob(context, details, TimeUnit.MINUTES.toMillis(3))
      case _ => requestWorkflow(context, details, env)
    }
  }

  private def requestWorkflow(context: JobExecutionContext, details: WorkflowExecution, env: Environment): ExtendedResult[Int] = {
    log.debug(s"Requesting automatic execution of ${details.workflow} workflow for env ${env.name} in project id = ${env.projectId}")

    val result = broker.requestWorkflow(details.envId, details.projectId, details.workflow, details.variables, "Scheduler / " + details.requestedBy)

    result match {
      case f: Failure =>
        notifyWorkflowRequestError(details, env, f)
        throw new JobExecutionException(s"Scheduled job failed to start workflow. ${toString(f)}")
      case r =>
        r
    }
  }

  private def rescheduleJob(context: JobExecutionContext, env: WorkflowExecution, delay: Long): Date = {
    log.debug(s"Environment ${env.envId} in project ${env.projectId} is in BUSY state. " +
      s"Postponing automatic ${env.workflow} execution for ${TimeUnit.MILLISECONDS.toMinutes(delay)} minute(s)}")

    val nextFireTrigger = context.getTrigger.getTriggerBuilder
      .startAt(new Date(System.currentTimeMillis() + delay))
      .build
    context.getScheduler.rescheduleJob(context.getTrigger.getKey, nextFireTrigger)
  }

  private def notifyWorkflowRequestError(details: WorkflowExecution, env: Environment, fail: Failure) {
    log.warn(s"Failed to execute workflow ${details.workflow} in environment ${env.name} Sending notification to creator.")

    notificationService.notifyCreator(env,
      subject = "[Genesis] Error: Genesis scheduled job execution error",
      message = s"""
        Hi,

        You received this message, because Genesis failed to run scheduled workflow job.
        Requested workflow: ${details.workflow}.


        Reason: ${toString(fail)}

        BR,
        Genesis
      """ )
  }


  def toString(f: Failure): String = {
    val error = new StringBuilder()
    if (f.compoundServiceErrors.nonEmpty || f.serviceErrors.nonEmpty)
      error.append("\nService errors: " + (f.compoundServiceErrors ++ f.serviceErrors.map{case (k,v) => s"$k: $v"}).reduce(_ + ", " + _))
    if (f.compoundVariablesErrors.nonEmpty || f.variablesErrors.nonEmpty)
      error.append("\nVariable errors: " + (f.compoundVariablesErrors ++ f.variablesErrors.map{case (k,v) => s"$k: $v"}).reduce(_ + ", " + _))
    error.toString()
  }
}




