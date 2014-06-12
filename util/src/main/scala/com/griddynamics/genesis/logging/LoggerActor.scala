/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.logging

import com.griddynamics.genesis.service.StoreService
import akka.actor._
import java.sql.Timestamp
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class LoggerActor(val service: StoreService) extends Actor {

  private val actionLogBuffer = new mutable.SynchronizedQueue[ActionBasedLog]
  private val stepLogBuffer = new mutable.SynchronizedQueue[Log]

  override def receive = {
    case log: Log => {
      stepLogBuffer += log
    }
    case log: ActionBasedLog => {
      actionLogBuffer += log
    }
    case Flush => {
      val actionLogWriter = context.system.actorOf(Props(new LogWriterActor(service)))
      actionLogWriter ! WriteActionBasedLogs(actionLogBuffer.dequeueAll(_ => true))
      actionLogWriter ! PoisonPill

      val stepLogWriter = context.system.actorOf(Props(new LogWriterActor(service)))
      stepLogWriter ! WriteStepLogs(stepLogBuffer.dequeueAll(_ => true))
      stepLogWriter ! PoisonPill
    }

  }
}

class LogWriterActor(val service: StoreService) extends Actor {
  override def receive = {
    case WriteStepLogs(stepLogs) => {
      service.writeLog(
        stepLogs.map{log => (log.stepId, log.message, log.timestamp)})
    }
    case WriteActionBasedLogs(actionLogs) => {
      service.writeActionLog(
        actionLogs.map{ log => (log.actionUID, log.message, log.timestamp)})
    }
  }
}

case class Log(stepId : Int, message: String, timestamp: Timestamp)

case class ActionBasedLog(actionUID: String, message: String, timestamp: Timestamp)

case class WriteActionBasedLogs(logs: Seq[ActionBasedLog])

case class WriteStepLogs(logs: Seq[Log])

case class Flush()

trait InternalLogger {
   def stepId : Int
   def writeLog(message : String) {
       LoggerWrapper.writeStepLog(stepId, message)
   }
}

class LoggerWrapper(logger: ActorRef) extends java.io.Serializable {
  import LoggerWrapper.now
  def writeActionLog(actionUUID: String, message: String, timestamp: Timestamp = now) {
    logger ! ActionBasedLog(actionUUID, message, timestamp)
  }

  def writeStepLog(id: Int, message: String, timestamp: Timestamp = now) {
    logger ! Log(id, message, timestamp)
  }

  def flush {
    logger ! Flush
  }
}

object LoggerWrapper {
  private var instance: LoggerWrapper = _
  private val ACTOR_NAME = "LoggerActor"

  private def now = new Timestamp(System.currentTimeMillis())

  def start(system: ActorSystem, storeService: StoreService) {
    val actor = system.actorOf(Props(new LoggerActor(storeService)), ACTOR_NAME)
    instance = new LoggerWrapper(actor)
    import ExecutionContext.Implicits.global
    system.scheduler.schedule(0 milliseconds, 5 seconds, actor, Flush)
  }

  def writeActionLog(actionUUID: String, message: String, timestamp: Timestamp = now) {
    instance.writeActionLog(actionUUID, message, timestamp)
  }

  def writeStepLog(id: Int, message: String, timestamp: Timestamp = now) {
    instance.writeStepLog(id, message, timestamp)
  }

  def flush {
    instance.flush
  }

  def logger() = instance
}