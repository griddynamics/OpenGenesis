package com.griddynamics.genesis.service

import java.sql.Timestamp
import com.griddynamics.genesis.model.StepLogEntry
import com.griddynamics.genesis.logs.{ActionBasedLog, Log}

trait LoggerService {
  def writeLog(log: Log)
  def writeLogs(logs: Seq[Log])
  def writeActionLog(log: ActionBasedLog)
  def writeActionLogs(logs: Seq[ActionBasedLog])
  def getLogs(stepId: Int, includeActions : Boolean) : Seq[StepLogEntry]
  def getLogs(actionUUID: String) : Seq[StepLogEntry]
  def storageMode: String
  def iterate(target: LoggerService)
}

object LoggerService {
  val FILESYSTEM = "filesystem"
  val DATABASE = "db"
}
