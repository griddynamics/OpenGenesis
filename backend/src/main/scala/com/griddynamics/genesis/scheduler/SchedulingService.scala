package com.griddynamics.genesis.scheduler

import java.util
import java.util.Date
import org.quartz.impl.matchers.GroupMatcher
import org.quartz._
import org.springframework.transaction.annotation.Transactional
import org.quartz.CronScheduleBuilder._
import org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule
import java.text.ParseException

trait SchedulingService {
  def removeAllScheduledJobs(projectId: Int, envId: Int)
  def removeScheduledJobs(executions: Seq[ExecutionId])
  def removeScheduledJob(execution: ExecutionId)
  def listJobs(projectId: Int, envId: Int): Seq[ExecutionSchedule]
  def listJobs(projectId: Int): Seq[ExecutionSchedule]
  def schedule(execution: Execution, date: Date, cronExpr: Option[String] = None)
  def getScheduledDate(execution: ExecutionId): Option[Date]
  def reschedule(execution: ExecutionId, newDate: Date, cronExpr: Option[String] = None): Option[Date]
  def getJob(projectId: Int, envId: Int, jobId: String): Execution
  def jobsPerProjectStat: Map[Int, Long]
}

class SchedulingServiceImpl(scheduler: Scheduler) extends SchedulingService {

  private val INTERVAL_REGEX = "([1-9][0-9]*)([mhdw])".r

  @Transactional
  def schedule(execution: Execution, date: Date, cronExpr: Option[String] = None) {
    val trigger = buildTrigger(execution, date, cronExpr)

    val jobDetail = JobBuilder.newJob(execution.jobClass)
      .withIdentity(execution.jobKey)
      .usingJobData(execution.toJobDataMap)
      .build()

    scheduler.scheduleJob(jobDetail, trigger)
  }

  import Execution.INTERVAL_UNITS
  private def intervalSchedule(expr: String) = (expr match {
    case INTERVAL_REGEX(i, x) if i.forall(_.isDigit) && INTERVAL_UNITS.contains(x) => calendarIntervalSchedule.withInterval(i.toInt, INTERVAL_UNITS(x))
    case _ => throw new ParseException("Schedule syntax is incorrect. Must be either a valid cron expression or interval duration", 0)
  }).preserveHourOfDayAcrossDaylightSavings(true)

  private def buildTrigger(execution: ExecutionId, date: Date, cronExpr: Option[String]): Trigger = {
    val builder = TriggerBuilder.newTrigger().withIdentity(execution.triggerKey).startAt(date)
    cronExpr.map(ce => {
      val schedule = if (CronExpression.isValidExpression(ce)) cronSchedule(ce) else intervalSchedule(ce)
      builder.withSchedule(schedule)
    }).getOrElse(builder).build()
  }

  @Transactional(readOnly = true)
  def getScheduledDate(execution: ExecutionId): Option[Date] = {
    Option(scheduler.getTrigger(execution.triggerKey)).flatMap (t => Option(t.getNextFireTime))
  }

  @Transactional
  def reschedule(execution: ExecutionId, newDate: Date, cronExpr: Option[String] = None): Option[Date] = {
    val trigger = Option(scheduler.getTrigger(execution.triggerKey))

    trigger.flatMap { t =>
      Option(scheduler.rescheduleJob(t.getKey, buildTrigger(execution, newDate, cronExpr)))
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
  def listJobs(projectId: Int, envId: Int) = {
    import scala.collection.JavaConversions._
    val triggerKeys = scheduler.getTriggerKeys(GroupIdentity.forEnv(projectId, envId))
    loadByTriggerKeys(triggerKeys.toSet)
  }

  private def loadByTriggerKeys(triggerKeys: Set[TriggerKey]) =
    triggerKeys.map { k =>
        val trigger = scheduler.getTrigger(k)
        val jobDetail = scheduler.getJobDetail(trigger.getJobKey)
        Execution(jobDetail, trigger)
    }.toSeq


  @Transactional(readOnly = true)
  def getJob(projectId: Int, envId: Int, jobId: String): Execution  = {
    val jobKey = new JobKey(jobId, GroupIdentity.forEnv(projectId, envId))
    Execution(scheduler.getJobDetail(jobKey))
  }

  @Transactional(readOnly = true)
  def listJobs(projectId: Int) = {
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
