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

package com.griddynamics.genesis.agent

import akka.actor.{ActorRef, Props, Actor}
import com.griddynamics.genesis.workflow._
import akka.event.Logging
import java.util.concurrent.ExecutorService
import com.griddynamics.genesis.workflow.agent.ExecutorActor
import com.griddynamics.genesis.agents.status.{GetStatus, StatusResponse}
import com.griddynamics.genesis.logging.LoggerWrapper

class FrontActor(actionToExec: Action => Option[ActionExecutor], execService: ExecutorService) extends Actor {
  import context.system
  val log = Logging(system, classOf[FrontActor])

  protected def receive = {

    case rt: RemoteTask => try {
      actionToExec(rt.action).foreach(sender ! executorActor(_, rt.supervisor, rt.logger))
    } catch {
      case t =>
        sender ! akka.actor.Status.Failure(t)
        log.error(t, "Error while processing remote task: %s", rt)
    }
    case GetStatus => sender ! new StatusResponse(0, 0)
    case m => log.debug("Unknown message: " + m)
  }

  def executorActor(executor: ActionExecutor, remote: ActorRef, logger: ActorRef) = {
    LoggerWrapper.registerRemote(logger)
    val asyncExecutor = executor match {
      case e: AsyncActionExecutor => e
      case e: SyncActionExecutor => new SyncActionExecutorAdapter(e, execService)
    }
    val config = system.settings.config
    val beatPeriodMs = config.getMilliseconds("beat.period")
    log.info("Agent beat period is: {}", beatPeriodMs)

    system.actorOf(Props(new ExecutorActor(asyncExecutor, remote, beatPeriodMs)))  //todo: not sure if we should create root level actors for tasks
  }
}
