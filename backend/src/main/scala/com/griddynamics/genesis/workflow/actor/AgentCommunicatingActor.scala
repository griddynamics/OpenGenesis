/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.workflow.actor

import akka.actor.{PoisonPill, Actor, ActorRef}
import akka.util.Timeout
import com.griddynamics.genesis.agents.AgentGateway
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.plugin.Cancel
import com.griddynamics.genesis.service.RemoteAgentsService
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.message.{Pong, Ping, Beat, Start}
import com.griddynamics.genesis.workflow.{ActionResult, RemoteTask, ActionFailed, Action}
import java.util.Date
import scala.concurrent.duration.FiniteDuration
import com.griddynamics.genesis.api.RemoteAgent
import scala.collection.mutable
import scala.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

case class ResolvingRemoteActorError(action: Action, override val desc: String) extends ActionFailed

case class RemoteAgentTimeout(action: Action) extends ActionFailed {
  override def desc = "Remote agent became unreachable"
}

class AgentCommunicatingActor(superVisor: ActorRef,
                              action: Action,
                              agentTag: String,
                              agentService: RemoteAgentsService,
                              logger: LoggerWrapper,
                              timeout: FiniteDuration) extends Actor with Logging{

  import context.dispatcher

  var executor: ActorRef = context.system.deadLetters
  val scheduler = context.system.scheduler
  var lastPongReceived: Date = new Date()
  case object CheckStatus
  case object CheckStatusTimeout
  case class CommandTimeout(hostname: String, remoteTask: RemoteTask)

  override def receive = {

    case Start =>
      submit(RemoteTask(action, self, logger, mutable.Map() withDefaultValue 0)) {
        agentService.getActiveAgent(agentTag)
      }

    case CommandTimeout(address, task) =>
      log.debug(s"Got timeout for address ${address}. Trying to send to another agent, if any")
      submit(task) {
        agentService.getActiveAgent(agentTag)
      }

    case Timeout =>
      LoggerWrapper.writeActionLog(action.uuid, s"Timeout: Remote agent did not reply within $timeout for job request. Check whether agent is working and configured correctly")
      superVisor ! new ResolvingRemoteActorError(action, "Timeout: couldn't submit job to remote agent")
      self ! PoisonPill

    case b: Beat =>
      log.trace("Received beat before received reference to executor")
      self forward b

    case executor: ActorRef =>
      log.trace("Received remote agent executor for an action. Sending Start command")
      this.executor = executor
      this.executor ! Start

      context.become(executorWatcher)

      this.executor ! Ping
      scheduler.schedule(timeout, timeout) {
        self ! CheckStatus
      }
  }

  def submit(task: RemoteTask)(future: => Future[Option[RemoteAgent]]) = for (agent <- future) yield doSubmit(task, agent)

  def doSubmit(task: RemoteTask, agent: Option[RemoteAgent]) = {
    agent match {
      case Some(a) => {
        if (task.history.exists({case (host, tryouts) => tryouts > 10}))
          failWithNoAgent()
        else
          submitRemoteJob(a, task)
      }
      case None => failWithNoAgent()
    }
  }

  def submitRemoteJob(a: RemoteAgent, task: RemoteTask) {
    log.debug(s"Submitting job to an agent ${a.hostname}:${a.port}. Waiting for remote executor")
    task.history(a.hostname) += 1
    AgentGateway.resolve(a) ! task
    scheduler.scheduleOnce(timeout) {
      self ! CommandTimeout(a.hostname, task)
    }
  }

  def failWithNoAgent()  {
    LoggerWrapper.writeActionLog(action.uuid, "Could not find working agent for tag: " + agentTag)
    superVisor ! new ResolvingRemoteActorError(action, "Could not find working remote agent for tag: " + agentTag)
    self ! PoisonPill
  }

  val executorWatcher: Receive = {
    case r: ActionResult =>
      log.trace("Got action result from remote executing agent")
      superVisor ! r
      self ! PoisonPill

    case CheckStatusTimeout =>
      executor ! Beat(Cancel()) //just in case
      superVisor ! RemoteAgentTimeout(action)
      self ! PoisonPill

    case CheckStatus =>
      if (System.currentTimeMillis() - this.lastPongReceived.getTime < (timeout.toMillis * 2))
        this.executor ! Ping
      else
        self ! CheckStatusTimeout

    case Pong =>
      this.lastPongReceived = new Date()

    case beat: Beat => executor ! beat

    case Timeout => /* ignore */

    case CommandTimeout => /* ignore too */

    case t => log.error(s" Remote agent watcher received unexpected message $t")
  }

}
