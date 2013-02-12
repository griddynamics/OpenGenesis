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

import akka.actor.{PoisonPill, Kill, Actor, ActorRef}
import akka.util.Timeout
import com.griddynamics.genesis.agents.AgentGateway
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.service.RemoteAgentsService
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.message.{Beat, Start}
import com.griddynamics.genesis.workflow.{ActionResult, RemoteTask, ActionFailed, Action}
import scala.concurrent.duration.FiniteDuration

case class ResolvingRemoteActorError(action: Action, override val desc: String) extends ActionFailed {
}

class AgentCommunicatingActor(superVisor: ActorRef,
                              action: Action,
                              agentTag: String,
                              agentService: RemoteAgentsService,
                              logger: LoggerWrapper,
                              timeout: FiniteDuration) extends Actor with Logging{
  import context.dispatcher

  var executor: ActorRef = _
  val scheduler = context.system.scheduler

  override def receive = {

    case Start =>
      val agent = agentService.findByTags(Seq(agentTag)).headOption

      agent match {
        case Some(a) =>
          log.trace("Submitting a job to an agent. Waiting for remote executor")

          AgentGateway.resolve(a) ! RemoteTask(action, self, logger)
          scheduler.scheduleOnce(timeout) { self ! Timeout }
        case None =>
          LoggerWrapper.writeActionLog(action.uuid, "Could not find agent for tag: " + agentTag)
          superVisor ! new ResolvingRemoteActorError(action, "Could not find remote agent for tag: " + agentTag)
          self ! PoisonPill
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
  }

  val executorWatcher: Receive = {
    case r: ActionResult =>
      log.trace("Got action result from remote executing agent")
      superVisor ! r
      self ! PoisonPill
    case beat: Beat => executor ! beat
    case Timeout => /* ignore */
    case t =>
      log.error(s"Remote agent watcher received unexpected message $t")
  }

}
