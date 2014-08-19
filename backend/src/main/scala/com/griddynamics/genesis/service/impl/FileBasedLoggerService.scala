package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.LoggerService
import java.sql.Timestamp
import com.griddynamics.genesis.model.StepLogEntry
import java.io.{FileFilter, FilenameFilter, FileWriter, File}
import com.griddynamics.genesis.annotation.RemoteGateway
import scala.io.Source
import org.apache.commons.io.{DirectoryWalker, FileUtils}
import org.apache.commons.io.filefilter.{FileFilterUtils, IOFileFilter}
import java.util
import com.griddynamics.genesis.logs.{ActionBasedLog, Log}

@RemoteGateway("Genesis filesystem access: FileBasedLoggerService")
class FileBasedLoggerService(directory: String) extends LoggerService {

  def storageMode = LoggerService.FILESYSTEM

  def writeLog(log: Log) {
    writeOne(log.stepId, log.timestamp, log.message)
  }

  def writeLogs(logs: Seq[Log])  {
    writeMore(logs map { log: Log => ((log.stepId, log.message, log.timestamp)) })
  }

  def writeActionLog(log: ActionBasedLog) {
    writeOne(log.actionUID, log.timestamp, log.message)
  }

  def writeActionLogs(logs: Seq[ActionBasedLog]) {
    writeMore(logs map {log: ActionBasedLog => ((log.actionUID, log.message, log.timestamp))} )
  }

  def getLogs(stepId: Int, includeActions : Boolean) : Seq[StepLogEntry] = {
    logs(stepId) {
      readStepEntry(stepId, _:String)
    }
  }

  def getLogs(actionUUID: String) : Seq[StepLogEntry] = {
    logs(actionUUID) {
      readActionEntry(actionUUID, _:String)
    }
  }


  def logs[B](id: B)(f: (String) => StepLogEntry) = {
    val logs: Option[Seq[StepLogEntry]] = logSource(id).map { s: Source => s.getLines().toList.map { line => {
      f(line)}}}
    logs.getOrElse(Seq())
  }


  def readActionEntry(actionUUID: String, line: String): StepLogEntry = {
    val fields = line.split("\t").toList
    new StepLogEntry(0, fields.tail.headOption.getOrElse(""), new Timestamp(fields.head.toLong), Some(actionUUID))
  }

  def readStepEntry(stepId: Int, line: String): StepLogEntry = {
    val fields = line.split("\t").toList
    new StepLogEntry(stepId, fields.tail.headOption.getOrElse(""), new Timestamp(fields.head.toLong), None)
  }

  private def logDirectory[B](stepId: B)(id: String) = new File(directory + File.separator + id + File.separator + stepId)
  private def actionLog(actionUUID: String) = {
    val dir: File = new File(
      logDirectory(actionUUID.substring(0, 2))(FileBasedLoggerService.actionsSubdir), actionUUID.substring(2, 4)
    )
    new File(
    dir, actionUUID)
  }

  private def aggreg(time: Timestamp): Long = {
    time.getTime / 1000
  }

  private def writeMore[B](logs: Seq[(B, String, Timestamp)]) {
    val grouped = logs.groupBy(_._1)
    grouped.foreach({case (id, tuple) => {
      val file = logWriter(id)
      tuple.foreach {
        case (_, message, time) => {
          file.append(String.valueOf(time.getTime)).append("\t").append(message).append("\n")
        }
      }
      file.close()
    }})
  }

  private def writeOne[B](stepId: B, timestamp: Timestamp, message: String) {
    val stamp = stepId match {
      case s: String => logWriter(s)
      case i: Int => logWriter(i)
    }
    stamp.append(String.valueOf(timestamp.getTime)).append("\t").append(message).append("\n")
    stamp.close()
  }

  private def logWriter[B](stepId: B) = {
    val path: String = logPath(stepId)
    new File(path).getParentFile.mkdirs()
    new FileWriter(path, true)
  }

  private def logSource[B](stepId: B): Option[Source] = {
    val file: File = new File(logPath(stepId))
    val result: Option[Source] = if (file.exists()){
      Some(Source.fromFile(file))
    } else  {
      None
    }
    result
  }

  def logPath[B](stepId: B) = {
    def top = logDirectory(stepId) _
    val logDir = stepId match {
      case s: String => actionLog(s)
      case o: Int => top(FileBasedLoggerService.stepsSubdir)
    }
    logDir.getAbsolutePath + ".log"
  }

  def iterate(to: LoggerService) = {
    val stepLogsDir = new File(directory + File.separator + FileBasedLoggerService.stepsSubdir)
    val actionLogsDir = new File(directory + File.separator + FileBasedLoggerService.actionsSubdir)

    def steps(f: File): Unit = {
      val stepId = f.getName.replaceAll("\\.log", "").toInt
      val lines = Source.fromFile(f).getLines().filter { _.trim.length() > 0 } map { readStepEntry(stepId, _)} map {entry: StepLogEntry => Log(stepId, entry.message, entry.timestamp)}
      to.writeLogs(lines.toSeq)
    }

    def actions(f: File) {
      val actionUUID = f.getName.replaceAll("\\.log", "")
      val lines = Source.fromFile(f).getLines().filter { _.trim.length() > 0 }.map { readActionEntry(actionUUID, _)} map {entry: StepLogEntry => ActionBasedLog(actionUUID, entry.message, entry.timestamp)}
      to.writeActionLogs(lines.toSeq)
    }

    readFromTop(stepLogsDir, steps)
    readFromTop(actionLogsDir, actions)
  }

  def readFromTop(dir: File, f: File => Unit) {
    import collection.JavaConversions._
    val files: util.Collection[File] = FileUtils.listFiles(dir, Array("log"), true)
    files.foreach { f }
  }
}

object FileBasedLoggerService {
  val stepsSubdir = "steps"
  val actionsSubdir = "actions"
}
