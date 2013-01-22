package com.griddynamics.genesis.scheduler.jobs

import org.quartz._
import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.model.{Environment, EnvStatus}
import java.util.Date
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.api.ExtendedResult
import com.griddynamics.genesis.scheduler.WorkflowExecution
import com.griddynamics.genesis.util.Logging

class WorkflowExecutionJob(broker: RequestBroker, storeService: StoreService) extends Job with Logging {

  def execute(context: JobExecutionContext) {
    val details = new WorkflowExecution(context.getMergedJobDataMap)
    val env = storeService.findEnv(details.envId, details.projectId).getOrElse{
      throw new IllegalStateException(s"Scheduling job ${details.toString} failed to perform execution because env wasn't found in database")
    }
    env.status match {
      case EnvStatus.Destroyed => /* do nothing */
      case EnvStatus.Busy => rescheduleJob(context, details, TimeUnit.MINUTES.toMillis(3))
      case _ => requestWorkflow(details, env)
    }
  }

  private def requestWorkflow(details: WorkflowExecution, env: Environment): ExtendedResult[Int] = {
    log.debug(s"Requesting automatic execution of ${details.workflow} workflow for env ${env.name} in project id = ${env.projectId}")

    val result = broker.requestWorkflow(details.envId, details.projectId, details.workflow, Map(), "Scheduled execution")
    if (!result.isSuccess) {
      throw new RuntimeException("Scheduled workflow executor failed to start workflow. RequestBroker request result = " + result)
    }
    result
  }

  private def rescheduleJob(context: JobExecutionContext, env: WorkflowExecution, delay: Long): Date = {
    log.debug(s"Environment ${env.envId} in project ${env.projectId} is in BUSY state. " +
      s"Postponing automatic ${env.workflow} execution for ${TimeUnit.MILLISECONDS.toMinutes(delay)} minute(s)}")

    val nextFireTrigger = context.getTrigger.getTriggerBuilder
      .startAt(new Date(System.currentTimeMillis() + delay))
      .build
    context.getScheduler.rescheduleJob(context.getTrigger.getKey, nextFireTrigger)
  }
}




