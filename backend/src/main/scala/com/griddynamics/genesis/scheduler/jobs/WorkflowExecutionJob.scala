package com.griddynamics.genesis.scheduler.jobs

import org.quartz._
import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.model.EnvStatus
import java.util.Date
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.api.ExtendedResult
import com.griddynamics.genesis.scheduler.WorkflowExecution

class WorkflowExecutionJob(broker: RequestBroker, storeService: StoreService) extends Job {

  def execute(context: JobExecutionContext) {
    val details = new WorkflowExecution(context.getMergedJobDataMap)
    val env = storeService.findEnv(details.envId, details.projectId).get
    env.status match {
      case EnvStatus.Destroyed => /* do nothing */
      case EnvStatus.Busy => rescheduleJob(context, TimeUnit.MINUTES.toMillis(3))
      case _ => requestWorkflow(details)
    }
  }

  private def requestWorkflow(details: WorkflowExecution): ExtendedResult[Int] = {
    val result = broker.requestWorkflow(details.envId, details.projectId, details.workflow, Map(), "Scheduled execution")
    if (!result.isSuccess) {
      throw new RuntimeException("Failed to start workflow. Result = " + result)
    }
    result
  }

  private def rescheduleJob(context: JobExecutionContext, delay: Long): Date = {
    val nextFireTrigger = context.getTrigger.getTriggerBuilder
      .startAt(new Date(System.currentTimeMillis() + delay))
      .build
    context.getScheduler.rescheduleJob(context.getTrigger.getKey, nextFireTrigger)
  }
}




