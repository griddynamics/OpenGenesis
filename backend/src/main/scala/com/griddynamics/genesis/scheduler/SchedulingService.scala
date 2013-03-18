package com.griddynamics.genesis.scheduler

import java.util
import java.util.Date
import org.quartz.impl.matchers.{EverythingMatcher, GroupMatcher}
import org.quartz.{TriggerKey, JobKey, JobBuilder, TriggerBuilder, Scheduler}
import org.springframework.transaction.annotation.Transactional

trait SchedulingService {
  def removeAllScheduledJobs(projectId: Int, envId: Int)
  def removeScheduledJobs(executions: Seq[ExecutionId])
  def removeScheduledJob(execution: ExecutionId)
  def listJobs(projectId: Int, envId: Int): Seq[(Date, Execution)]
  def listJobs(projectId: Int): Seq[(Date, Execution)]
  def schedule(execution: Execution, date: Date)
  def getScheduledDate(execution: ExecutionId): Option[Date]
  def reschedule(execution: ExecutionId, newDate: Date): Option[Date]
  def getJob(projectId: Int, envId: Int, jobId: String): Execution
  def jobsPerProjectStat: Map[Int, Long]
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
    val keys = scheduler.getJobKeys(GroupIdentity.forEnv(projectId, envId))
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

  @Transactional(readOnly = true)
  def listJobs(projectId: Int, envId: Int): Seq[(Date, Execution)] = {
    import scala.collection.JavaConversions._
    val triggerKeys = scheduler.getTriggerKeys(GroupIdentity.forEnv(projectId, envId))
    loadByTriggerKeys(triggerKeys.toSet)
  }

  private def loadByTriggerKeys(triggerKeys: Set[TriggerKey]): Seq[(Date, Execution)] = {
    triggerKeys.map { k =>
        val trigger = scheduler.getTrigger(k)
        val jobDetail = scheduler.getJobDetail(trigger.getJobKey)
        (trigger.getNextFireTime, Execution(jobDetail))
    }.toSeq
  }

  @Transactional(readOnly = true)
  def getJob(projectId: Int, envId: Int, jobId: String): Execution  = {
    val jobKey = new JobKey(jobId, GroupIdentity.forEnv(projectId, envId))
    Execution(scheduler.getJobDetail(jobKey))
  }

  @Transactional(readOnly = true)
  def listJobs(projectId: Int): Seq[(Date, Execution)] = {
    import scala.collection.JavaConversions._
    val keys = scheduler.getTriggerKeys(GroupMatcher.groupStartsWith(GroupIdentity.forProjectPrefix(projectId)))
    loadByTriggerKeys(keys.toSet)
  }

  @Transactional(readOnly = true)
  def jobsPerProjectStat: Map[Int, Long] = {      // Map (ProjectId -> Requested Jobs count)
    import GroupIdentity._
    import Execution._
    import scala.collection.JavaConversions._

    val keys = scheduler.getTriggerKeys(GroupMatcher.groupStartsWith(GroupIdentity.basePrefix))
    keys.
      filter( isWorkflowExecution ).
      groupBy ( projectId ).
      map { case (projectId, triggerKeys) => (projectId, triggerKeys.size.toLong) }
  }

  private implicit def triggerMatcher(groupId: String): GroupMatcher[TriggerKey] = GroupMatcher.triggerGroupEquals(groupId)
  private implicit def groupMatcher(groupId: String): GroupMatcher[JobKey] = GroupMatcher.jobGroupEquals(groupId)
}
