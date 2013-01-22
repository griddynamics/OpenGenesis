package com.griddynamics.genesis.scheduler.jobs

import com.griddynamics.genesis.model.{Environment, EnvStatus}
import com.griddynamics.genesis.scheduler.{NotificationService, DestructionCheck}
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.util.Logging
import java.util.Date
import java.util.concurrent.TimeUnit
import org.quartz._

class DestructionStatusCheckJob(storeService: StoreService,
                                notificationService: NotificationService) extends Job with Logging {

  def execute(context: JobExecutionContext) {
    val execution = new DestructionCheck(context.getMergedJobDataMap)
    val env = storeService.findEnv(execution.envId, execution.projectId).getOrElse{
      throw new IllegalStateException(s"Scheduling job ${execution.toString} failed to perform execution because env wasn't found in database")
    }
    env.status match {
      case EnvStatus.Destroyed => notifyEnvDestroyed(env)
      case EnvStatus.Busy => rescheduleCheck(context, env)
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
      rescheduleCheck(context, env)
    } else {
      notifyDestructionFailure(env)
    }
  }

  private def rescheduleCheck(context: JobExecutionContext, env: Environment ) {
    log.debug(s"Environment automatic destruction for ${env.name} in project ${env.projectId} rescheduling check because of env being in BUSY state")

    val inFiveMinutes = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toSeconds(5))
    val newTrigger = context.getTrigger.getTriggerBuilder.startAt(inFiveMinutes).build()
    context.getScheduler.rescheduleJob(context.getTrigger.getKey, newTrigger)
  }

  private def notifyDestructionFailure(env: Environment) {
    log.warn(s"Environment ${env.name} wasn't destroyed according scheduling plan. Sending notification to creator.")

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
    log.debug(s"Environment ${env.name} was successfully destroyed according scheduling plan.")

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