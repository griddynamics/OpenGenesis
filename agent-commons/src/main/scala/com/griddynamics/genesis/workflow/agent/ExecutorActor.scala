/*
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
package com.griddynamics.genesis.workflow.agent

import com.griddynamics.genesis.util.{SafeOperation, Logging}
import com.griddynamics.genesis.workflow._
import akka.actor.{Cancellable, ActorRef, PoisonPill, Actor}
import com.griddynamics.genesis.logging.LoggerWrapper
import akka.event.Logging
import message._
import scala.Some
import com.griddynamics.genesis.workflow.action.{ExecutorInterrupt, ExecutorThrowable}
import signal.Success

import org.apache.commons.lang3.exception.ExceptionUtils
import scala.concurrent.duration._
import concurrent.ExecutionContext

class ExecutorActor(unsafeExecutor: AsyncActionExecutor,
                    supervisor: ActorRef,
                    beatPeriodMs: Long,
                    logger: LoggerWrapper) extends Actor {
  import context.dispatcher
  val log = Logging(context.system, this.getClass)

  private val safeExecutor = new SafeAsyncActionExecutor(unsafeExecutor, logger)

  private var cancellable: Cancellable = _

  override def receive = {
    case Start => {
      log.debug("Starting async executor for '{}'", safeExecutor.action)
      safeExecutor.startAsync()
      cancellable = context.system.scheduler.schedule(0 milliseconds, beatPeriodMs.intValue() milliseconds, self, Beat(Success()))
    }
    case Beat(Success()) => {
      safeExecutor.getResult match {
        case Some(result) => {
          log.debug("Async executor for '%s' finished with result '%s'".format(safeExecutor.action, result))
          supervisor ! result
          log.debug("Sent result to '%s'".format(supervisor))
          safeExecutor.cleanUp(Success())
          finish()
        }
        case None =>
      }
    }
    case Beat(signal) => {
      log.debug("Async executor for '%s' interrupted with signal '%s'", safeExecutor.action, signal)
      if (safeExecutor.canRespond(signal)) {
        log.debug("Executor %s can process signal %s", safeExecutor, signal)
        safeExecutor.cleanUp(signal)
        supervisor ! ExecutorInterrupt(safeExecutor.action, signal)
        finish()
      } else {
        supervisor ! 'Delayed
        log.debug("Signal %s cannot be processed with executor %s right now", signal, safeExecutor)
      }
    }
  }

  def finish() {
    //beatSource.unsubscribe(self, self ! PoisonPill)
    cancellable.cancel()
    self ! PoisonPill
    becomeIgnoreBeat()
  }

  def becomeIgnoreBeat() {
    context.become {
      case Beat(_) => ()
    }
  }
}

class SafeAsyncActionExecutor(unsafeExecutor: AsyncActionExecutor, logger: LoggerWrapper) extends AsyncActionExecutor with Logging {
  val action = unsafeExecutor.action

  var result: Option[ActionResult] = None

  override def canRespond(signal : Signal) = unsafeExecutor.canRespond(signal)

  private def handleError(t: Throwable, method: String) {
    logThrowable(t, method)
    result = Some(ExecutorThrowable(unsafeExecutor.action, t))
  }

  def startAsync() = SafeOperation{unsafeExecutor.startAsync}{handleError(_, "startAsync")}

  def getResult = {
    result orElse SafeOperation {result = unsafeExecutor.getResult}{handleError(_, "getResult")}
    result
  }

  def cleanUp(signal: Signal) {
    SafeOperation {unsafeExecutor.cleanUp(signal)}  {logThrowable(_, "cleanUp", signal) }
  }

  private def logThrowable(t: Throwable, method: String, signal: Signal = null) {
    val signalMsg = if(signal != null) " and signal '%s'".format(signal) else ""
    log.warn(t, "Throwable while %s for action '%s'%s", method, action, signalMsg)
    logger.writeActionLog(action.uuid, "Throwable while %s%s: %s".format(method, signalMsg, t.getMessage))
    logger.writeActionLog(action.uuid, "Stack trace:\n %s".format(ExceptionUtils.getStackTrace(t)))
  }
}
