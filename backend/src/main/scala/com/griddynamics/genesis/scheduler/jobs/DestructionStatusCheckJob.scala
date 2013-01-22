package com.griddynamics.genesis.scheduler.jobs

import com.griddynamics.genesis.service.{EmailService, StoreService}
import org.quartz._
import com.griddynamics.genesis.model.{Environment, EnvStatus}
import java.util.concurrent.TimeUnit
import java.util.Date
import com.griddynamics.genesis.scheduler.{NotificationService, DestructionCheck}
import com.griddynamics.genesis.configuration.MailServiceContext
import com.griddynamics.genesis.users.UserService

class DestructionStatusCheckJob(storeService: StoreService,
                                notificationService: NotificationService) extends Job {

  def execute(context: JobExecutionContext) {
    val execution = new DestructionCheck(context.getMergedJobDataMap)
    val env = storeService.findEnv(execution.envId, execution.projectId).getOrElse{
      throw new IllegalStateException(s"Scheduling job ${execution.toString} failed to perform execution because env wasn't found in database")
    }
    env.status match {
      case EnvStatus.Destroyed => notifyEnvDestroyed(env)
      case EnvStatus.Busy => rescheduleCheck(context)
      case _ => checkState(execution, env, context)
    }
  }

  private def checkState(execution: DestructionCheck, env: Environment, context: JobExecutionContext) {
    def wasntRun(t: Trigger): Boolean = {
      Option(t.getPreviousFireTime).isEmpty
    }

    val trigger = Option(context.getScheduler.getTrigger(execution.destructionTriggerKey))

    val fired = trigger match {
      case Some(t) => wasntRun(t)
      case None => true
    }

    if (!fired) {
      rescheduleCheck(context)
    } else {
      notifyDestructionFailure(env)
    }
  }

  private def rescheduleCheck(context: JobExecutionContext) {
    val inFiveMinutes = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toSeconds(5))
    val newTrigger = context.getTrigger.getTriggerBuilder.startAt(inFiveMinutes).build()
    context.getScheduler.rescheduleJob(context.getTrigger.getKey, newTrigger)
  }

  private def notifyDestructionFailure(env: Environment) {
    notificationService.notifyCreator(env,
      subject = "[Genesis] Error: Genesis automatic instance destruction failure",
      message = s"""
        Hi,

        You received this message, because Genesis failed to destroy instance '${env.name}' according to schedule.

        BR,
        Genesis
      """ )
  }

  private def notifyEnvDestroyed(env: Environment) {
    notificationService.notifyCreator(env,
      subject = s"[Genesis] Instance '${env.name}' was successfully destroyed according to schedule plan",
      message = s"""
        Hi,

        Genesis automaticly destroyed instance '${env.name}' according to schedule plan

        BR,
        Genesis
      """ )
  }
}