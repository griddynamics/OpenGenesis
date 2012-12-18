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
 */
package com.griddynamics.genesis.agents

import com.griddynamics.genesis.service.{GenesisSystemProperties, ConfigService, AgentsHealthService}
import akka.actor._
import com.griddynamics.genesis.api.RemoteAgent
import akka.dispatch.{Future, Await}
import akka.util.duration.intToDurationInt
import akka.pattern.{AskTimeoutException, ask}
import com.griddynamics.genesis.api.AgentStatus._
import akka.util.Timeout
import java.util.concurrent.TimeoutException
import com.griddynamics.genesis.util.Logging
import akka.actor.SupervisorStrategy.Resume

class AgentsHealthServiceImpl(actorSystem: ActorSystem, configService: ConfigService)
  extends AgentsHealthService with Logging{

  private val agentPollingPeriod = configService.get(GenesisSystemProperties.AGENT_POLLING_PERIOD, 30)

  private val tracker = actorSystem.actorOf(Props(new Actor with ActorLogging {
    val child = context.actorOf(Props(new StatusTrackerRoot(agentPollingPeriod)))

    protected def receive = {
      case m => child.forward(m)
    }

    override def supervisorStrategy() = {
     def decider: SupervisorStrategy.Decider = { case _: Exception => Resume }
     OneForOneStrategy()(decider)
    }
  }))

  implicit val requestTimeout = Timeout(1 seconds)

  def checkStatus(agent: RemoteAgent) = try {
    val future = (tracker ? GetAgentStatus(agent)).mapTo[AgentStatus]
    Await.result(future, requestTimeout.duration)
  } catch {
    case te: TimeoutException => Unavailable
  }

  def checkStatus(agents: Seq[RemoteAgent]): Seq[(RemoteAgent, AgentStatus)] = {
    val futures = agents.map { a =>
      val statusFuture = (tracker ? GetAgentStatus(a)).mapTo[AgentStatus]
      statusFuture.recover {
        case e: AskTimeoutException  => Unavailable
      }.map ((a, _))
    }
    implicit val context = actorSystem.dispatcher
    Await.result(Future.sequence(futures), 2 seconds)
  }

  def stopTracking(agent: RemoteAgent) {
    tracker ! StopTracking(agent)
  }

  def startTracking(agent: RemoteAgent) {
    tracker ! StartTracking(agent)
  }
}



