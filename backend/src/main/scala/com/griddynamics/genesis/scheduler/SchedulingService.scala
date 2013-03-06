package com.griddynamics.genesis.scheduler

import java.util
import java.util.Date
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.{TriggerKey, JobKey, JobBuilder, TriggerBuilder, Scheduler}
import org.springframework.transaction.annotation.Transactional

trait SchedulingService {
  def removeAllScheduledJobs(projectId: Int, envId: Int)
  def removeScheduledJobs(executions: Seq[ExecutionId])
  def removeScheduledJob(execution: ExecutionId)
  def listJobs(projectId: Int, envId: Int): Seq[(Date, Execution)]
  def schedule(execution: Execution, date: Date)
  def getScheduledDate(execution: ExecutionId): Option[Date]
  def reschedule(execution: ExecutionId, newDate: Date): Option[Date]
  def getJob(envId: Int, jobId: String): Execution
}

class SchedulingServiceImpl(scheduler: Scheduler) extends SchedulingService {

  @Transactional
  def schedule(execution: Execution, date: Date) {
    val trigger = TriggerBuilder.newTrigger().withIdentity(execution.triggerKey).startAt(date).build()

    val jobDetail = JobBuilder.newJob(execution.jobClass)
      .withIdentity(execution.jobKey)
      .usingJobData(execution.toJobDataMap)
      .build()

    scheduler.scheduleJob(jobDetail, trigger)
  }

  @Transactional(readOnly = true)
  def getScheduledDate(execution: ExecutionId): Option[Date] = {
    Option(scheduler.getTrigger(execution.triggerKey)).flatMap (t => Option(t.getNextFireTime))
  }

  @Transactional
  def reschedule(execution: ExecutionId, newDate: Date): Option[Date] = {
    val trigger = Option(scheduler.getTrigger(execution.triggerKey))

    trigger.flatMap { t =>
      val newTrigger = t.getTriggerBuilder.startAt(newDate).build()
      Option(scheduler.rescheduleJob(t.getKey, newTrigger))
    }
  }

  @Transactional
  def removeAllScheduledJobs(projectId: Int, envId: Int) {
    val keys = scheduler.getJobKeys(GroupIdentity.forEnv(envId))
    scheduler.deleteJobs(new util.ArrayList[JobKey](keys))
  }

  @Transactional
  def removeScheduledJob(execution: ExecutionId) {
    scheduler.deleteJob(execution.jobKey)
  }

  @Transactional
  def removeScheduledJobs(executions: Seq[ExecutionId]) {
    import scala.collection.JavaConversions._
    scheduler.deleteJobs(executions.map(_.jobKey))
  }

  @Transactional
  def listJobs(projectId: Int, envId: Int): Seq[(Date, Execution)] = {
    import scala.collection.JavaConversions._
    val triggerKeys = scheduler.getTriggerKeys(GroupIdentity.forEnv(envId))
    triggerKeys.map { k =>
      val trigger = scheduler.getTrigger(k)
      val jobDetail = scheduler.getJobDetail(trigger.getJobKey)
      (trigger.getNextFireTime,  Execution(jobDetail))
    }.toSeq
  }

  @Transactional
  def getJob(envId: Int, jobId: String): Execution  = {
    val jobKey = new JobKey(jobId, GroupIdentity.forEnv(envId))
    Execution(scheduler.getJobDetail(jobKey))
  }


  private implicit def triggerMatcher(groupId: String): GroupMatcher[TriggerKey] = GroupMatcher.triggerGroupEquals(groupId)
  private implicit def groupMatcher(groupId: String): GroupMatcher[JobKey] = GroupMatcher.jobGroupEquals(groupId)
}
