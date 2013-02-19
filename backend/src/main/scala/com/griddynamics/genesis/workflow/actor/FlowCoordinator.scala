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

import java.util.concurrent.ExecutorService
import com.griddynamics.genesis.workflow
import com.griddynamics.genesis.util.Logging
import collection.mutable
import workflow.message.{Beat, Start}
import workflow.signal.{Rescue, Fail, TimeOut, Success}
import workflow.{StepResult, Signal}
import akka.actor._
import com.griddynamics.genesis.common.Mistake
import scala.Left
import scala.Right
import com.griddynamics.genesis.service.RemoteAgentsService
import com.griddynamics.genesis.configuration.WorkflowConfig

class FlowCoordinator(unsafeFlowCoordinator: workflow.FlowCoordinator,
                      executorService: ExecutorService,
                      remoteAgentService: RemoteAgentsService,
                      beatSource: BeatSource, config: WorkflowConfig) extends Actor
with FlowActor with Logging {
    val safeFlowCoordinator = new SafeFlowCoordinator(unsafeFlowCoordinator)

    var finishSignal: Signal = Success()

    val stepCoordinators = mutable.Set[ActorRef]()
    val finalCoordinators = mutable.Set[ActorRef]()

    override def receive = {
        case Start => {
            log.debug("Starting flow '%s'", safeFlowCoordinator.flowDescription)
            beatSource.subscribeOnce(self, Beat(TimeOut()), config.flowTimeOutMs)
            processFlowInstruction(safeFlowCoordinator.onFlowStart())
        }
        case Beat(signal) if signal != Success() => {
            log.debug("Flow '%s' was interrupted by signal '%s'",
                safeFlowCoordinator.flowDescription, signal)
            interruptFlow(signal)
        }
        case result: StepResult => {
            log.debug("Result is %s (regular state)", result)
            removeCoordinator(sender)
            processFlowInstruction(safeFlowCoordinator.onStepFinish(result))
        }
    }

    val interrupted: Receive = {
        case Beat(signal) if signal == Rescue() => {
            log.debug("Rescue signal got")
            attemptToFinish()
        }
        case Beat(signal) if signal != Success() && signal != Rescue() => {
            log.debug("Signal '%s' was ignored because flow '%s' was already interrupted by signal '%s'",
                signal, safeFlowCoordinator.flowDescription, finishSignal)
        }
        case result: StepResult => {
            log.debug("Result is %s (interrupted state)", result)
            removeCoordinator(sender)
            safeFlowCoordinator.onStepFinish(result)
            attemptToFinish()
        }
    }

    def processFlowInstruction(instruction: Either[Signal, Seq[workflow.StepCoordinator]]) {
        instruction match {
            case Left(signal) => {
                if (safeFlowCoordinator.hasRescue) {
                    startRescueCoordinators()
                }
                interruptFlow(signal)
            }
            case Right(coordinators) => {
                startCoordinators(coordinators)
                attemptToFinish()
            }
        }
    }

    def startRescueCoordinators() {
        safeFlowCoordinator.rescueCoordinators.right.foreach(startCoordinators(_, rescue = true))
    }

    def interruptFlow(signal: Signal) {
        log.debug("Interrupting flow with signal %s", signal)
        finishSignal = signal
        beatCoordinators(Beat(signal))
        context.become(interrupted)
        attemptToFinish()
    }

    def attemptToFinish() {
        if (stepCoordinators.isEmpty && finalCoordinators.isEmpty) {
            beatSource.unsubscribeOnce(self, {
                beatSource.tryToFinish()
                self ! PoisonPill
            })

            log.debug("Flow '%s' was finished with signal '%s'",
                safeFlowCoordinator.flowDescription, finishSignal)

            safeFlowCoordinator.onFlowFinish(finishSignal)
            becomeIgnoreBeat()
        }
    }

    def removeCoordinator(actorRef: ActorRef) {
        stepCoordinators.remove(actorRef)
        finalCoordinators.remove(actorRef)
    }

    def beatCoordinators(beat: Beat) {
        log.debug("Pinging coordinators with %s", beat)
        for (coordinator <- stepCoordinators)
            // TODO exception was looked during sync executor exception
            coordinator ! beat
    }

    def startCoordinators(coordinators: Seq[workflow.StepCoordinator], rescue: Boolean = false) {
        for (coordinator <- coordinators)
            startCoordinator(coordinator, rescue)
    }

    def startCoordinator(coordinator: workflow.StepCoordinator, rescue: Boolean = false) {
        log.debug("Starting coordinator %s", coordinator.getClass.getName)
        val stepCoordinatorActor = context.actorOf(Props(new StepCoordinator(coordinator, self, executorService, remoteAgentService, beatSource, config, rescue)))
        if (rescue)
            finalCoordinators += stepCoordinatorActor
        else
            stepCoordinators += stepCoordinatorActor
        stepCoordinatorActor ! Start
    }
}

class SafeFlowCoordinator(unsafeFlowCoordinator: workflow.FlowCoordinator)
    extends workflow.FlowCoordinator with Logging {

    def flowDescription =
        try {
            unsafeFlowCoordinator.flowDescription
        } catch {
            case t: Throwable => {
                val systemName = unsafeFlowCoordinator.getClass.getName + "@" +
                    java.lang.System.identityHashCode(unsafeFlowCoordinator)
                log.warn(t, "Throwable while flowDescription for flow coordinator %s", systemName)
                systemName
            }
        }

    def onStepFinish(result: StepResult) =
        try {
            unsafeFlowCoordinator.onStepFinish(result)
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while onStepFinish for flow '%s' and step result '%s'",
                    flowDescription, result)
                Left(Fail(Mistake(t)))
            }
        }

    def onFlowFinish(signal: Signal) {
        try {
            unsafeFlowCoordinator.onFlowFinish(signal)
        }
        catch {
            case t: Throwable => log.warn(t, "Throwable while onFlowFinish for flow '%s' and signal '%s'",
                flowDescription, signal)
        }
    }

    def onFlowStart() =
        try {
            unsafeFlowCoordinator.onFlowStart()
        } catch {
            case t: Throwable => {
                log.warn(t, "Throwable while onFlowStart for flow '%s'", flowDescription)
                Left(Fail(Mistake(t)))
            }
        }
    def rescueCoordinators = unsafeFlowCoordinator.rescueCoordinators

    def hasRescue = unsafeFlowCoordinator.hasRescue
}

//TODO think about proper flow return, results, callbacks
trait TypedFlowCoordinator {
    def start()

    def signal(signal: Signal)
}

class TypedFlowCoordinatorImpl(flowCoordinator: workflow.FlowCoordinator,
                               workflowConfig: WorkflowConfig,
                               executorService: ExecutorService, actorSystem: ActorSystem, remoteAgentService: RemoteAgentsService)
  extends TypedFlowCoordinator with Logging {
    val beatSource : BeatSource = {
      val props: TypedProps[BeatSource] = TypedProps(classOf[BeatSource], {new BeatSourceImpl(workflowConfig.beatPeriodMs)})
      TypedActor(actorSystem).typedActorOf(props)
    }

    val flowCoordinatorActor = actorSystem.actorOf(Props(new FlowCoordinator(flowCoordinator, executorService, remoteAgentService, beatSource, workflowConfig)))

    def start() {
        beatSource.start()
        flowCoordinatorActor ! Start
    }

    def signal(signal: Signal) {
        flowCoordinatorActor ! Beat(signal)
    }
}
