package com.griddynamics.genesis.scheduler.jobs

import com.griddynamics.genesis.model.{EnvStatus, Environment}
import com.griddynamics.genesis.scheduler.{NotificationService, ExpireNotification}
import com.griddynamics.genesis.service.StoreService
import java.util.Date
import java.util.concurrent.TimeUnit
import org.quartz.{JobExecutionContext, Job}

class NotificationJob(storeService: StoreService,
                      notificationService: NotificationService) extends Job {

  def execute(context: JobExecutionContext) {
    val execution = new ExpireNotification(context.getMergedJobDataMap)

    for {
      env <- storeService.findEnv(execution.envId, execution.projectId) if env.status != EnvStatus.Destroyed
    } {
      notifyCreator(env, execution.destroyDate)
    }
  }

  private def notifyCreator( env: Environment, destroyDate: Date) {
    notificationService.notifyCreator(env,
      subject = s"[Genesis] Attention: Genesis instance ${env.name} will be automatically destroyed",
      message =
        s"""
            Hello,

            This is a friendly reminder, that your instance ${env.name} will be automatically destroyed in ${TimeUnit.MILLISECONDS.toHours(destroyDate.getTime)} hours.\n\n

            BR,
            Genesis Notification system
        """
    )
  }

//  private def notifyAdmins(emails: Seq[String], destroyDate: Date)(implicit emailService: EmailService) {
    //todo
//  }

}