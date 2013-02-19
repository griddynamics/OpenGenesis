package com.griddynamics.genesis.model.scheduling

import org.squeryl.{Table, KeyedEntity, Schema}
import org.squeryl.dsl.{CompositeKey2, CompositeKey3}

case class JobDetails(SCHED_NAME: String,
                      JOB_NAME: String,
                      JOB_GROUP: String,
                      DESCRIPTION: Option[String],
                      JOB_CLASS_NAME: String,
                      IS_DURABLE: String,
                      IS_NONCONCURRENT: String,
                      IS_UPDATE_DATA: String,
                      REQUESTS_RECOVERY: String,
                      JOB_DATA: Option[Array[Byte]]) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(SCHED_NAME, JOB_NAME, JOB_GROUP)

  def this() = this("", "", "", Some(""), "", "", "", "", "", Some(Array(1.asInstanceOf[Byte])) )
}

case class Trigger(SCHED_NAME: String,
                   TRIGGER_NAME: String,
                   TRIGGER_GROUP: String,
                   JOB_NAME: String,
                   JOB_GROUP: String,
                   DESCRIPTION: Option[String],
                   NEXT_FIRE_TIME: Option[Long],
                   PREV_FIRE_TIME: Option[Long],
                   PRIORITY: Option[Int],
                   TRIGGER_STATE: String,
                   TRIGGER_TYPE: String,
                   START_TIME: Long,
                   END_TIME: Option[Long],
                   CALENDAR_NAME: Option[String],
                   MISFIRE_INSTR: Option[Int],
                   JOB_DATA: Option[Array[Byte]]) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)

  def this() =
    this("","", "", "", "", Some(""), Some(Long.MaxValue), Some(Long.MaxValue),
      Some(1), "", "", 1, Some(Long.MaxValue), Some(""), Some(1), Some(Array(Byte.box(1))))
}

case class SimpleTrigger( SCHED_NAME: String,
                          TRIGGER_NAME: String,
                          TRIGGER_GROUP: String,
                          REPEAT_COUNT: Long,
                          REPEAT_INTERVAL: Long,
                          TIMES_TRIGGERED: Long) extends KeyedEntity[CompositeKey3[String, String, String]] {
  def id = new CompositeKey3(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
}

case class CronTrigger( SCHED_NAME: String,
                        TRIGGER_NAME: String,
                        TRIGGER_GROUP: String,
                        CRON_EXPRESSION: String,
                        TIME_ZONE_ID: Option[String]) extends KeyedEntity[CompositeKey3[String, String, String]] {
  def id = new CompositeKey3(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)

  def this() = this("", "", "", "", Some(""))
}

case class SimpropTrigger ( SCHED_NAME: String,
                            TRIGGER_NAME: String,
                            TRIGGER_GROUP: String,
                            STR_PROP_1: Option[String],
                            STR_PROP_2: Option[String],
                            STR_PROP_3: Option[String],
                            INT_PROP_1: Option[Int],
                            INT_PROP_2: Option[Int],
                            LONG_PROP_1: Option[Long],
                            LONG_PROP_2: Option[Long],
                            DEC_PROP_1: Option[BigDecimal],
                            DEC_PROP_2: Option[BigDecimal],
                            BOOL_PROP_1: Option[String],
                            BOOL_PROP_2: Option[String] ) extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)

  def this() = this("", "", "", Some(""), Some(""), Some(""), Some(1), Some(1),
    Some(Long.MaxValue), Some(Long.MaxValue), Some(BigDecimal(1)), Some(BigDecimal(1)), Some(""), Some(""))
}

case class BlobTrigger(SCHED_NAME: String,
                  TRIGGER_NAME: String,
                  TRIGGER_GROUP: String,
                  BLOB_DATA: Option[Array[Byte]])  extends KeyedEntity[CompositeKey3[String, String, String]] {

  def id = new CompositeKey3(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)

  def this() = this("", "", "", Some(Array(Byte.box(1))))
}

case class Calendar(SCHED_NAME: String,
               CALENDAR_NAME: String,
               CALENDAR: Array[Byte]) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(SCHED_NAME, CALENDAR_NAME)
  def this() = this("", "", Array(Byte.box(1)))
}

case class PausedTriggerGrps(SCHED_NAME: String,
                        TRIGGER_GROUP: String) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(SCHED_NAME, TRIGGER_GROUP)

}

case class FiredTrigger(SCHED_NAME: String,
                   ENTRY_ID: String,
                   TRIGGER_NAME: String,
                   TRIGGER_GROUP: String,
                   INSTANCE_NAME: String,
                   FIRED_TIME: Long,
                   PRIORITY: Int,
                   STATE: String,
                   JOB_NAME: Option[String],
                   JOB_GROUP: Option[String],
                   IS_NONCONCURRENT: Option[String],
                   REQUESTS_RECOVERY: Option[String]) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(SCHED_NAME, ENTRY_ID)

  def this() = this("", "", "", "", "", 0, 0, "", Some(""), Some(""), Some(""), Some(""))

}

case class SchedulerState(SCHED_NAME: String,
                     INSTANCE_NAME: String,
                     LAST_CHECKIN_TIME: Long,
                     CHECKIN_INTERVAL: Long) extends KeyedEntity[CompositeKey2[String, String]] {

  def id = new CompositeKey2(SCHED_NAME, INSTANCE_NAME)
}

case class Lock(SCHED_NAME: String, LOCK_NAME: String) extends KeyedEntity[CompositeKey2[String, String]] {
  def id = new CompositeKey2(SCHED_NAME, LOCK_NAME)
}

trait QuartzSchema extends Schema {
  import org.squeryl.PrimitiveTypeMode._
  import org.squeryl.dsl.ast.ConstantExpressionNode
  import org.squeryl.dsl.BinaryExpression

  implicit def createLeafNodeOfScalarBinaryOptionType(i: Option[BinaryType]) = {
    new ConstantExpressionNode[Option[BinaryType]](i) with BinaryExpression[Option[BinaryType]]
  }

  val details = table[JobDetails]("QRTZ_JOB_DETAILS")
  val triggers = table[Trigger]("QRTZ_TRIGGERS")
  val simpleTriggers = table[SimpleTrigger]("QRTZ_SIMPLE_TRIGGERS")
  val cronTriggers = table[CronTrigger]("QRTZ_CRON_TRIGGERS")
  val simpropTriggers = table[SimpropTrigger]("QRTZ_SIMPROP_TRIGGERS")
  val blobTriggers = table[BlobTrigger]("QRTZ_BLOB_TRIGGERS")
  val calendars = table[Calendar]("QRTZ_CALENDARS")
  val pausedTriggerGrps = table[PausedTriggerGrps]("QRTZ_PAUSED_TRIGGER_GRPS")
  val firedTriggers = table[FiredTrigger]("QRTZ_FIRED_TRIGGERS")
  val schedulerState = table[SchedulerState]("QRTZ_SCHEDULER_STATE")
  val locks = table[Lock]("QRTZ_LOCKS")

// TODO: no referential integrity declared because of https://www.assembla.com/spaces/squeryl/tickets/25-compositekeys-cannot-be-the-binding-expression-for-relations#/activity/ticket:

  on(details)(d => declare(
    columns(d.SCHED_NAME, d.JOB_NAME, d.JOB_GROUP) are (unique, primaryKey),
    d.SCHED_NAME is (dbType("varchar(120)")),
    d.JOB_NAME is (dbType("varchar(200)")),
    d.JOB_GROUP is (dbType("varchar(200)")),
    d.DESCRIPTION is (dbType("varchar(250)")),
    d.JOB_CLASS_NAME is (dbType("varchar(250)")),
    d.IS_DURABLE is (dbType("varchar(1)")),
    d.IS_NONCONCURRENT is (dbType("varchar(1)")),
    d.IS_UPDATE_DATA is (dbType("varchar(1)")),
    d.REQUESTS_RECOVERY is (dbType("varchar(1)"))//,
//    d.JOB_DATA is (dbType("blob"))
  ))

  on(triggers)(t => declare(
    columns(t.SCHED_NAME,t.TRIGGER_NAME,t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.JOB_NAME is (dbType("varchar(200)")),
    t.JOB_GROUP is (dbType("varchar(200)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)")),
    t.DESCRIPTION is (dbType("varchar(250)")),
    t.TRIGGER_STATE is (dbType("varchar(16)")),
    t.TRIGGER_TYPE is (dbType("varchar(8)")),
    t.CALENDAR_NAME is (dbType("varchar(200)"))//,
//    t.JOB_DATA is (dbType("blob"))
  ))

  on(simpleTriggers)(t => declare(
    columns(t.SCHED_NAME,t.TRIGGER_NAME,t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)"))
  ))

  on(cronTriggers)(t => declare(
    columns(t.SCHED_NAME,t.TRIGGER_NAME,t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)")),
    t.CRON_EXPRESSION is (dbType("varchar(120)")),
    t.TIME_ZONE_ID is (dbType("varchar(80)"))
  ))

  on(simpropTriggers)(t => declare(
    columns(t.SCHED_NAME,t.TRIGGER_NAME,t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)")),
    t.STR_PROP_1 is (dbType("varchar(500)")),
    t.STR_PROP_2 is (dbType("varchar(500)")),
    t.STR_PROP_3 is (dbType("varchar(500)")),
    t.BOOL_PROP_1 is (dbType("varchar(5)")),
    t.BOOL_PROP_2 is (dbType("varchar(5)"))
  ))

  on(blobTriggers)(t => declare(
    columns(t.SCHED_NAME,t.TRIGGER_NAME,t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)"))
  ))

  on(calendars)(t => declare(
      columns(t.SCHED_NAME,t.CALENDAR_NAME) are (unique, primaryKey),
      t.SCHED_NAME is (dbType("varchar(120)")),
      t.CALENDAR_NAME is (dbType("varchar(200)"))
  ))

  on(pausedTriggerGrps)(t => declare(
    columns(t.SCHED_NAME, t.TRIGGER_GROUP) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)"))
  ))

  on(firedTriggers)(t => declare(
    columns(t.SCHED_NAME, t.ENTRY_ID) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.ENTRY_ID is (dbType("varchar(95)")),
    t.TRIGGER_NAME is (dbType("varchar(200)")),
    t.TRIGGER_GROUP is (dbType("varchar(200)")),
    t.INSTANCE_NAME is (dbType("varchar(200)")),
    t.STATE is (dbType("varchar(16)")),
    t.JOB_NAME is (dbType("varchar(200)")),
    t.JOB_GROUP is (dbType("varchar(200)")),
    t.IS_NONCONCURRENT is (dbType("varchar(5)")),
    t.REQUESTS_RECOVERY is (dbType("varchar(5)"))
  ))

  on(schedulerState)(t => declare(
    columns(t.SCHED_NAME, t.INSTANCE_NAME) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.INSTANCE_NAME is (dbType("varchar(200)"))
  ))

  on(locks)(t => declare(
    columns(t.SCHED_NAME, t.LOCK_NAME) are (unique, primaryKey),
    t.SCHED_NAME is (dbType("varchar(120)")),
    t.LOCK_NAME is (dbType("varchar(40)"))
  ))
}
