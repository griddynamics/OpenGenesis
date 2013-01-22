package com.griddynamics.genesis.model.scheduling

import org.squeryl.{Table, KeyedEntity, Schema}
import org.squeryl.dsl.{CompositeKey2, CompositeKey3}

case class JobDetails(sched_name: String,
                      job_name: String,
                      job_group: String,
                      description: Option[String],
                      job_class_name: String,
                      is_durable: String,
                      is_nonconcurrent: String,
                      is_update_data: String,
                      requests_recovery: String,
                      job_data: Option[Array[Byte]]) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(sched_name, job_name, job_group)

  def this() = this("", "", "", Some(""), "", "", "", "", "", Some(Array(1.asInstanceOf[Byte])) )
}

case class Trigger(sched_name: String,
                   trigger_name: String,
                   trigger_group: String,
                   job_name: String,
                   job_group: String,
                   description: Option[String],
                   next_fire_time: Option[Long],
                   prev_fire_time: Option[Long],
                   priority: Option[Int],
                   trigger_state: String,
                   trigger_type: String,
                   start_time: Long,
                   end_time: Option[Long],
                   calendar_name: Option[String],
                   misfire_instr: Option[Int],
                   job_data: Option[Array[Byte]]) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(sched_name, trigger_name, trigger_group)

  def this() =
    this("","", "", "", "", Some(""), Some(Long.MaxValue), Some(Long.MaxValue),
      Some(1), "", "", 1, Some(Long.MaxValue), Some(""), Some(1), Some(Array(Byte.box(1))))
}

case class SimpleTrigger( sched_name: String,
                          trigger_name: String,
                          trigger_group: String,
                          repeat_count: Long,
                          repeat_interval: Long,
                          times_triggered: Long) extends KeyedEntity[CompositeKey3[String, String, String]] {
  def id = new CompositeKey3(sched_name, trigger_name, trigger_group)
}

case class CronTrigger( sched_name: String,
                        trigger_name: String,
                        trigger_group: String,
                        cron_expression: String,
                        time_zone_id: Option[String]) extends KeyedEntity[CompositeKey3[String, String, String]] {
  def id = new CompositeKey3(sched_name, trigger_name, trigger_group)

  def this() = this("", "", "", "", Some(""))
}

case class SimpropTrigger ( sched_name: String,
                            trigger_name: String,
                            trigger_group: String,
                            str_prop_1: Option[String],
                            str_prop_2: Option[String],
                            str_prop_3: Option[String],
                            int_prop_1: Option[Int],
                            int_prop_2: Option[Int],
                            long_prop_1: Option[Long],
                            long_prop_2: Option[Long],
                            dec_prop_1: Option[BigDecimal],
                            dec_prop_2: Option[BigDecimal],
                            bool_prop_1: Option[String],
                            bool_prop_2: Option[String] ) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(sched_name, trigger_name, trigger_group)

  def this() = this("", "", "", Some(""), Some(""), Some(""), Some(1), Some(1),
    Some(Long.MaxValue), Some(Long.MaxValue), Some(BigDecimal(1)), Some(BigDecimal(1)), Some(""), Some(""))
}

case class BlobTrigger(sched_name: String,
                  trigger_name: String,
                  trigger_group: String,
                  blob_data: Option[Array[Byte]])  extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(sched_name, trigger_name, trigger_group)

  def this() = this("", "", "", Some(Array(Byte.box(1))))
}

case class Calendar(sched_name: String,
               calendar_name: String,
               calendar: Array[Byte]) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(sched_name, calendar_name)
  def this() = this("", "", Array(Byte.box(1)))
}

case class PausedTriggerGrps(sched_name: String,
                        trigger_group: String) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(sched_name, trigger_group)

}

case class FiredTrigger(sched_name: String,
                   entry_id: String,
                   trigger_name: String,
                   trigger_group: String,
                   instance_name: String,
                   fired_time: Long,
                   priority: Int,
                   state: String,
                   job_name: Option[String],
                   job_group: Option[String],
                   is_nonconcurrent: Option[String],
                   requests_recovery: Option[String]) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(sched_name, entry_id)

  def this() = this("", "", "", "", "", 0, 0, "", Some(""), Some(""), Some(""), Some(""))

}

case class SchedulerState(sched_name: String,
                     instance_name: String,
                     last_checkin_time: Long,
                     checkin_interval: Long) extends KeyedEntity[CompositeKey2[String, String]] {
  def id = new CompositeKey2(sched_name, instance_name)
}

case class Lock(sched_name: String, lock_name: String) extends KeyedEntity[CompositeKey2[String, String]] {
  def id = new CompositeKey2(sched_name, lock_name)
}

trait QuartzSchema extends Schema {
  import org.squeryl.PrimitiveTypeMode._
  import org.squeryl.dsl.ast.ConstantExpressionNode
  import org.squeryl.dsl.BinaryExpression

  implicit def createLeafNodeOfScalarBinaryOptionType(i: Option[BinaryType]) = {
    new ConstantExpressionNode[Option[BinaryType]](i) with BinaryExpression[Option[BinaryType]]
  }

  val details = table[JobDetails]("qrtz_job_details")
  val triggers = table[Trigger]("qrtz_triggers")
  val simpleTriggers = table[SimpleTrigger]("qrtz_simple_triggers")
  val cronTriggers = table[CronTrigger]("qrtz_cron_triggers")
  val simpropTriggers = table[SimpropTrigger]("qrtz_simprop_triggers")
  val blobTriggers = table[BlobTrigger]("qrtz_blob_triggers")
  val calendars = table[Calendar]("qrtz_calendars")
  val pausedTriggerGrps = table[PausedTriggerGrps]("qrtz_paused_trigger_grps")
  val firedTriggers = table[FiredTrigger]("qrtz_fired_triggers")
  val schedulerState = table[SchedulerState]("qrtz_scheduler_state")
  val locks = table[Lock]("qrtz_locks")


// TODO: no referential integrity declared because of https://www.assembla.com/spaces/squeryl/tickets/25-compositekeys-cannot-be-the-binding-expression-for-relations#/activity/ticket:

  on(details)(d => declare(
    columns(d.sched_name, d.job_name, d.job_group) are (unique, primaryKey),
    d.sched_name is (dbType("varchar(120)")),
    d.job_name is (dbType("varchar(200)")),
    d.job_group is (dbType("varchar(200)")),
    d.description is (dbType("varchar(250)")),
    d.job_class_name is (dbType("varchar(250)")),
    d.is_durable is (dbType("varchar(1)")),
    d.is_nonconcurrent is (dbType("varchar(1)")),
    d.is_update_data is (dbType("varchar(1)")),
    d.requests_recovery is (dbType("varchar(1)"))//,
//    d.job_data is (dbType("blob"))
  ))

  on(triggers)(t => declare(
    columns(t.sched_name,t.trigger_name,t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.job_name is (dbType("varchar(200)")),
    t.job_group is (dbType("varchar(200)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)")),
    t.description is (dbType("varchar(250)")),
    t.trigger_state is (dbType("varchar(16)")),
    t.trigger_type is (dbType("varchar(8)")),
    t.calendar_name is (dbType("varchar(200)"))//,
//    t.job_data is (dbType("blob"))
  ))

  on(simpleTriggers)(t => declare(
    columns(t.sched_name,t.trigger_name,t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)"))
  ))

  on(cronTriggers)(t => declare(
    columns(t.sched_name,t.trigger_name,t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)")),
    t.cron_expression is (dbType("varchar(120)")),
    t.time_zone_id is (dbType("varchar(80)"))
  ))

  on(simpropTriggers)(t => declare(
    columns(t.sched_name,t.trigger_name,t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)")),
    t.str_prop_1 is (dbType("varchar(500)")),
    t.str_prop_2 is (dbType("varchar(500)")),
    t.str_prop_3 is (dbType("varchar(500)")),
    t.bool_prop_1 is (dbType("varchar(5)")),
    t.bool_prop_2 is (dbType("varchar(5)"))
  ))

  on(blobTriggers)(t => declare(
    columns(t.sched_name,t.trigger_name,t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)"))
  ))

  on(calendars)(t => declare(
      columns(t.sched_name,t.calendar_name) are (unique, primaryKey),
      t.sched_name is (dbType("varchar(120)")),
      t.calendar_name is (dbType("varchar(200)"))
  ))

  on(pausedTriggerGrps)(t => declare(
    columns(t.sched_name, t.trigger_group) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.trigger_group is (dbType("varchar(200)"))
  ))

  on(firedTriggers)(t => declare(
    columns(t.sched_name, t.entry_id) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.entry_id is (dbType("varchar(95)")),
    t.trigger_name is (dbType("varchar(200)")),
    t.trigger_group is (dbType("varchar(200)")),
    t.instance_name is (dbType("varchar(200)")),
    t.state is (dbType("varchar(16)")),
    t.job_name is (dbType("varchar(200)")),
    t.job_group is (dbType("varchar(200)")),
    t.is_nonconcurrent is (dbType("varchar(5)")),
    t.requests_recovery is (dbType("varchar(5)"))
  ))

  on(schedulerState)(t => declare(
    columns(t.sched_name, t.instance_name) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.instance_name is (dbType("varchar(200)"))
  ))

  on(locks)(t => declare(
    columns(t.sched_name, t.lock_name) are (unique, primaryKey),
    t.sched_name is (dbType("varchar(120)")),
    t.lock_name is (dbType("varchar(40)"))
  ))
}
