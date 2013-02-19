package com.griddynamics.genesis.scheduler

import java.util
import java.util.Date
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.{JobKey, JobBuilder, JobDetail, TriggerBuilder, Trigger, Scheduler}

trait SchedulingService {
  def removeAllScheduledJobs(projectId: Int, envId: Int)
  def schedule(execution: Execution, date: Date)
  def getScheduledDate(execution: ExecutionId): Option[Date]
  def reschedule(execution: ExecutionId, newDate: Date): Option[Date]
}

class SchedulingServiceImpl(scheduler: Scheduler) extends SchedulingService {

  def schedule(execution: Execution, date: Date) {
    val trigger = TriggerBuilder.newTrigger().withIdentity(execution.triggerKey).startAt(date).build()

    val jobDetail = JobBuilder.newJob(execution.jobClass)
      .withIdentity(execution.jobKey)
      .usingJobData(execution.toJobDataMap)
      .build()

    scheduler.scheduleJob(jobDetail, trigger)
  }

  def getScheduledDate(execution: ExecutionId): Option[Date] = {
    Option(scheduler.getTrigger(execution.triggerKey)).flatMap (t => Option(t.getNextFireTime))
  }

  def reschedule(execution: ExecutionId, newDate: Date): Option[Date] = {
    val trigger = Option(scheduler.getTrigger(execution.triggerKey))

    trigger.flatMap { t =>
      val newTrigger = t.getTriggerBuilder.startAt(newDate).build()
      Option(scheduler.rescheduleJob(t.getKey, newTrigger))
    }
  }

  def removeAllScheduledJobs(projectId: Int, envId: Int) {
    val keys = scheduler.getJobKeys(GroupIdentity.forEnv(envId))
    scheduler.deleteJobs(new util.ArrayList[JobKey](keys))
  }

  private implicit def groupMatcher(groupId: String): GroupMatcher[JobKey] = GroupMatcher.jobGroupEquals(groupId)
}
