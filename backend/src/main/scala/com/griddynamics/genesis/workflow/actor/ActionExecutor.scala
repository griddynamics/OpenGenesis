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
package com.griddynamics.genesis.workflow.actor

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.action._
import com.griddynamics.genesis.workflow.message._
import com.griddynamics.genesis.workflow.{ActionResult, AsyncActionExecutor, Signal}
import akka.actor.{ActorRef, PoisonPill, Actor}
import com.griddynamics.genesis.workflow.signal.Success

class ActionExecutor(unsafeExecutor: AsyncActionExecutor,
                     supervisor: ActorRef,
                     beatSource: BeatSource) extends Actor with FlowActor with Logging {
    private val safeExecutor = new SafeAsyncActionExecutor(unsafeExecutor)

    protected def receive = {
        case Start => {
            log.debug("Starting async executor for '%s'", safeExecutor.action)
            safeExecutor.startAsync()
            beatSource.subscribe(self, Beat(Success()))
        }
        case Beat(Success()) => {
            safeExecutor.getResult match {
                case Some(result) => {
                    log.debug("Async executor for '%s' finished with result '%s'", safeExecutor.action, result)
                    supervisor ! result
                    safeExecutor.cleanUp(Success())
                    finish()
                }
                case None => ()
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
       beatSource.unsubscribe(self, self ! PoisonPill)
       becomeIgnoreBeat()
    }
}

class SafeAsyncActionExecutor(unsafeExecutor: AsyncActionExecutor) extends AsyncActionExecutor
with Logging {
    val action = unsafeExecutor.action

    var result: Option[ActionResult] = None

    override def canRespond(signal : Signal) = unsafeExecutor.canRespond(signal)

    def startAsync() =
        try {
            unsafeExecutor.startAsync
        } catch {
            case t => {
                log.warn(t, "Throwable while startAsync for action '%s'", action)
                result = Some(ExecutorThrowable(unsafeExecutor.action, t))
            }
        }

    def getResult = result match {
        case Some(_) => result
        case None => {
            try {
                result = unsafeExecutor.getResult
            } catch {
                case t => {
                    log.warn(t, "Throwable while getResult for action '%s'", action)
                    result = Some(ExecutorThrowable(unsafeExecutor.action, t))
                }
            }
            result
        }
    }

    def cleanUp(signal: Signal) {
        try {
            unsafeExecutor.cleanUp(signal)
        }
        catch {
            case t => log.warn(t, "Throwable while cleanUp for action '%s' and signal '%s'",
                action, signal)
        }
    }
}
