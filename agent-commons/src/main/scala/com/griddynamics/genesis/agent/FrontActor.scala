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

import akka.actor.{Terminated, ActorRef, Props, Actor}
import com.griddynamics.genesis.workflow._
import akka.event.Logging
import java.util.concurrent.ExecutorService
import com.griddynamics.genesis.workflow.agent.ExecutorActor
import com.griddynamics.genesis.agents.status.{StatusResponse, GetStatus}
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.agents.configuration.{ConfigurationResponse, ConfigurationApplied, ApplyConfiguration, GetConfiguration}

class FrontActor(actionToExec: Action => Option[ActionExecutor], execService: ExecutorService, configService: SimpleConfigService) extends Actor {
  import context.system
  val log = Logging(system, classOf[FrontActor])
  var running = 0
  var total = 0

  override def receive = {
    case rt@RemoteTask(action, supervisor, logger) => try {
      actionToExec(action).foreach(a => {
        val actor = executorActor(a, supervisor, logger)
        context.watch(actor)
        running += 1
        total += 1
        sender ! actor
      })
    } catch {
      case t: Throwable =>
        sender ! akka.actor.Status.Failure(t)
        log.error(t, "Error while processing remote task: %s", rt)
    }
    case GetStatus =>
      sender ! StatusResponse(running, total)
    case Terminated(_) =>
      running -= 1
    case GetConfiguration => {
      log.debug("Get configuration call")
      sender ! ConfigurationResponse(configService.getConfig)
    }
    case ApplyConfiguration(values) =>
      sender ! configService.applyConfiguration(values)
    case m => log.debug("Unknown message: " + m)
  }

  def executorActor(executor: ActionExecutor, remote: ActorRef, logger: LoggerWrapper) = {
    val asyncExecutor = executor match {
      case e: AsyncActionExecutor => e
      case e: SyncActionExecutor => new SyncActionExecutorAdapter(e, execService)
    }
    val config = system.settings.config
    val beatPeriodMs = config.getMilliseconds("beat.period")
    log.info("Agent beat period is: {}", beatPeriodMs)

    system.actorOf(Props(new ExecutorActor(asyncExecutor, remote, beatPeriodMs, logger)))  //todo: not sure if we should create root level actors for tasks
  }
}
