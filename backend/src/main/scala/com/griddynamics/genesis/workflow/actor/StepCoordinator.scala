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

import scala.collection.mutable
import scala.concurrent.duration._
import com.griddynamics.genesis.workflow
import com.griddynamics.genesis.workflow.message._
import java.util.concurrent.ExecutorService
import akka.actor._
import akka.actor.SupervisorStrategy._
import com.griddynamics.genesis.workflow._
import workflow.action.{DelayedExecutorInterrupt, ExecutorInterrupt}
import scala.Some
import workflow.signal.{Rescue, Fail, Success}
import workflow.step.{CoordinatorThrowable, CoordinatorInterrupt}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.common.Mistake
import com.griddynamics.genesis.service.RemoteAgentsService
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.configuration.WorkflowConfig
import akka.util.Timeout

class StepCoordinator(unsafeStepCoordinator: workflow.StepCoordinator,
                      supervisor: ActorRef,
                      executorService: ExecutorService,
                      remoteAgentService: RemoteAgentsService,
                      beatSource: BeatSource, config: WorkflowConfig,
                      isRescue: Boolean  = false) extends Actor with FlowActor with Logging {
    val safeStepCoordinator = new SafeStepCoordinator(unsafeStepCoordinator)

    var interruptSignal: Signal = if (isRescue)
        Rescue()
    else
        Success() // Success() signal only for log messages

    private val regularExecutors = mutable.Map[ActorRef, Action]()
    private val signalExecutors = mutable.Map[ActorRef, Action]()
  private val executorFailures = mutable.Map[ActorRef, Throwable]()

    override def receive = {
        case Start => {
            log.debug("Starting step '%s'", safeStepCoordinator.step)

            startExecutors(safeStepCoordinator.onStepStart(), regularExecutors)
            attemptToRegularFinish()
        }
        case beat@Beat(signal) if signal != Success() => {
            log.debug("Step '%s' was interrupted by signal '%s'",
                safeStepCoordinator.step, signal)

            interruptSignal = signal
            beatExecutors(beat, regularExecutors)

            context.become(interrupted)
            startExecutors(safeStepCoordinator.onStepInterrupt(signal), signalExecutors)
            attemptToSignalFinish()
        }
        case result: ActionResult => {
            removeExecutor(sender, regularExecutors)
            startExecutors(safeStepCoordinator.onActionFinish(result), regularExecutors)
            attemptToRegularFinish()
        }
        case Terminated(a) => handleTermination(a, regularExecutors)
    }

    val interrupted: Receive = {
        case 'Delayed => {
          log.debug("Signal processing delayed")
        }
        case Beat(signal) if signal != Success() => {
            log.debug("Signal '%s' was ignored because '%s' was already interrupted by signal '%s'",
                signal, safeStepCoordinator.step, interruptSignal)
        }
        case result: ActionResult if isExecutorPresented(sender, regularExecutors) => {
            log.debug("Got ActionResult in interrupted state with presenting executors.")
            removeExecutor(sender, regularExecutors)
            val interruptResult = DelayedExecutorInterrupt(result.action, result, interruptSignal)
            startExecutors(safeStepCoordinator.onActionFinish(interruptResult), signalExecutors)
            attemptToSignalFinish()
        }
        case result@ExecutorInterrupt(action, _) => {
            removeExecutor(sender, regularExecutors)
            startExecutors(safeStepCoordinator.onActionFinish(result), signalExecutors)
            attemptToSignalFinish()
        }
        case result: ActionResult => {
            removeExecutor(sender, signalExecutors)
            startExecutors(safeStepCoordinator.onActionFinish(result), signalExecutors)
            attemptToSignalFinish()
        }
        case Terminated(a) => handleTermination(a, signalExecutors)
    }

    def attemptToRegularFinish() {
        if (regularExecutors.isEmpty) {
            val stepResult = safeStepCoordinator.getStepResult()

            supervisor ! stepResult
            self ! PoisonPill

            log.debug("Step '%s' was finished with result '%s'",
                safeStepCoordinator.step, stepResult)

            becomeIgnoreBeat()
        }
    }

    def attemptToSignalFinish() {
        if (regularExecutors.isEmpty && signalExecutors.isEmpty) {
            supervisor ! CoordinatorInterrupt(safeStepCoordinator.step, interruptSignal)
            self ! PoisonPill

            log.debug("Step '%s' was interrupted by signal '%s'",
                safeStepCoordinator.step, interruptSignal)

            becomeIgnoreBeat()
        }
    }

    def isExecutorPresented(actorRef: ActorRef, executorsPool: mutable.Map[ActorRef, Action]) = {
        executorsPool.contains(actorRef)
    }

    def removeExecutor(actorRef: ActorRef, executorsPool: mutable.Map[ActorRef, Action]) {
      executorsPool.remove(actorRef)
      context.unwatch(actorRef)
    }

    def beatExecutors(beat: Beat, executorsPool: mutable.Map[ActorRef, Action]) {
        for (executor <- executorsPool.keys)
            executor ! beat
    }

    def startExecutors(executors: Seq[workflow.ActionExecutor], executorsPool: mutable.Map[ActorRef, Action]) {
        for (executor <- executors)
            startExecutor(executor, executorsPool)
    }

    def startExecutor(executor: workflow.ActionExecutor, executorsPool: mutable.Map[ActorRef, Action]) {
        val asyncExecutor = executor match {
            case e: AsyncActionExecutor => e
            case e: SyncActionExecutor =>
                new SyncActionExecutorAdapter(e, executorService)
        }
      val actionExecutionActor = executorActor(asyncExecutor)
      context.watch(actionExecutionActor)
      actionExecutionActor ! Start
      executorsPool(actionExecutionActor) = executor.action
    }

  private def executorActor(asyncExecutor: AsyncActionExecutor): ActorRef = asyncExecutor.action match {
    case r: RemoteAgentExec if r.tag.nonEmpty =>
      context.actorOf(Props(remoteExecutor(r.tag, asyncExecutor.action)))
    case _ =>
      context.actorOf(Props(new actor.ActionExecutor(asyncExecutor, self, beatSource)))
   }

  private val TIMEOUT_REMOTE_ACTOR = Timeout(config.remoteExecutorWaitTimeout  seconds)

  private def remoteExecutor(tag: String, action: Action): AgentCommunicatingActor = {
    val logger = action match {
      case al: ActionWithLog => al.logger
      case _ => LoggerWrapper.logger()
    }
    new AgentCommunicatingActor(self, action, tag, remoteAgentService, logger, TIMEOUT_REMOTE_ACTOR.duration)
  }

  // do not restart executor actors on unhandled exception in receive
  // restarting with cleared state could lead to unpredictable behavior
  override def supervisorStrategy = OneForOneStrategy() {
    case e: Exception => executorFailures(sender) = e
      Stop
  }

  private def handleTermination(a: ActorRef, executorsPool: mutable.Map[ActorRef, Action]) {
    executorFailures.get(a).foreach(t => {
      val result = new ActionFailed {
        val action = executorsPool.getOrElse(a, new Action{ val desc = "Unknown action"})

        val desc = s"Executor actor has thrown unexpected exception: $t. See log for details."
      }
      removeExecutor(a, executorsPool)
      startExecutors(safeStepCoordinator.onActionFinish(result), executorsPool)
      if (executorsPool eq regularExecutors) attemptToRegularFinish else attemptToSignalFinish
    })
  }
}

class SafeStepCoordinator(unsafeStepCoordinator: workflow.StepCoordinator)
    extends workflow.StepCoordinator with Logging {

    val step = unsafeStepCoordinator.step

    var exceptionalResult: Option[StepResult] = None

    def onStepStart() =
        try {
            unsafeStepCoordinator.onStepStart()
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while onStepStart for step '%s'", step)
                exceptionalResult = Some(CoordinatorThrowable(step, t))
                this.onStepInterrupt(Fail(Mistake(t)))
            }
        }

    def onStepInterrupt(signal: Signal) =
        try {
            unsafeStepCoordinator.onStepInterrupt(signal)
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while onStepInterrupt for step '%s' and signal '%s'",
                    step, signal)
                Seq()
            }
        }

    def onActionFinish(actionResult: ActionResult) =
        try {
            unsafeStepCoordinator.onActionFinish(actionResult)
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while onActionFinish for step '%s' and action result '%s'",
                    step, actionResult)

                if (exceptionalResult.isEmpty)
                    exceptionalResult = Some(CoordinatorThrowable(step, t))

                Seq()
            }
        }

    def getStepResult() =
        try {
            exceptionalResult.getOrElse { unsafeStepCoordinator.getStepResult() }
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while getStepResult for step '%s'", step)
                CoordinatorThrowable(step, t)
            }
        }
}
