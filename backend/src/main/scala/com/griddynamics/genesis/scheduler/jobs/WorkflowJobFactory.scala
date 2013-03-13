package com.griddynamics.genesis.scheduler.jobs

import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.scheduler.NotificationService
import com.griddynamics.genesis.service.StoreService
import org.quartz.spi.{TriggerFiredBundle, JobFactory}
import org.quartz.{SchedulerException, Scheduler}
import scala.Predef._

class WorkflowJobFactory(broker: RequestBroker, storeService: StoreService, notificationService: NotificationService) extends JobFactory {

  def newJob(bundle: TriggerFiredBundle, scheduler: Scheduler) = {
    val jobClass = bundle.getJobDetail.getJobClass

    try {
      if (jobClass == classOf[WorkflowExecutionJob])
        new WorkflowExecutionJob(broker, storeService, notificationService)
      else if (jobClass == classOf[DestructionStatusCheckJob])
        new DestructionStatusCheckJob(storeService, notificationService)
      else if (jobClass == classOf[NotificationJob])
        new NotificationJob(storeService, notificationService)
      else
        jobClass.newInstance()
    } catch {
      case e: Exception => throw new SchedulerException(s"Scheduler job factory couldn't instantiate job class '${jobClass.getName}'", e)
    }
  }
}
