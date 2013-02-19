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

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.service.StoreService
import akka.actor._
import java.sql.Timestamp

class LoggerActor(val service: StoreService) extends Actor with Logging {
  override def receive = {
    case Log(id, message, timestamp) => {
      service.writeLog(id, message, timestamp)
    }
    case ActionBasedLog(actionUUID, message, timestamp) => {
      service.writeActionLog(actionUUID, message, timestamp)
    }
  }
}

case class Log(stepId : Int, message: String, timestamp: Timestamp)

case class ActionBasedLog(actionUID: String, message: String, timestamp: Timestamp)

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
}

object LoggerWrapper extends Logging {
  private var instance: LoggerWrapper = _
  private val ACTOR_NAME = "LoggerActor"

  private def now = new Timestamp(System.currentTimeMillis())

  def start(system: ActorSystem, storeService: StoreService) {
    instance = new LoggerWrapper(system.actorOf(Props(new LoggerActor(storeService)), ACTOR_NAME))
  }

  def writeActionLog(actionUUID: String, message: String, timestamp: Timestamp = now) {
    instance.writeActionLog(actionUUID, message, timestamp)
  }

  def writeStepLog(id: Int, message: String, timestamp: Timestamp = now) {
    instance.writeStepLog(id, message, timestamp)
  }

  def logger() = instance
}