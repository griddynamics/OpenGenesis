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

import akka.actor._
import akka.remote._
import akka.actor.FSM.Transition
import akka.actor.FSM.CurrentState
import akka.actor.FSM.SubscribeTransitionCallBack
import akka.remote.RemoteClientConnected
import akka.remote.RemoteClientDisconnected

import com.griddynamics.genesis.api.AgentStatus._
import com.griddynamics.genesis.api
import api.{JobStats, RemoteAgent}
import com.griddynamics.genesis.agents.status.{GetStatus, StatusResponse}
import scala.concurrent.duration._

case class GetAgentStatus(agent: RemoteAgent)
case class StartTracking(agent: RemoteAgent)
case class StopTracking(agent: RemoteAgent)

class StatusTrackerRoot(pollingPeriod: Int) extends Actor {
  import scala.collection.mutable

  private val trackerActors: mutable.Map[Address, ActorRef] = new mutable.HashMap[Address, ActorRef]()

  private val trackerState: mutable.Map[ActorRef, AgentStatus] = new mutable.HashMap[ActorRef, AgentStatus]()
  private val trackerStats: mutable.Map[ActorRef, JobStats] = new mutable.HashMap[ActorRef, JobStats]()

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[RemoteClientLifeCycleEvent])
    super.preStart()
  }

  private def createTracker(agent: RemoteAgent): ActorRef = {
    context.actorOf(Props(new StatusTracker(agent, pollingPeriod)))
  }

  override def receive = {
    case m@RemoteClientConnected(remote, address) => lookupTracker(address).forward(m)
    case m@RemoteClientDisconnected(remote, address) => lookupTracker(address).forward(m)

    case Transition(ref, oldState: AgentStatus, newState: AgentStatus) => trackerState(ref) = newState
    case CurrentState(ref, currentStatus: AgentStatus) => trackerState(ref) = currentStatus

    case GetAgentStatus(agent) => {
      val tracker = lookupTracker(agent)
      sender ! (trackerState(tracker), trackerStats.get(tracker))
    }

    case StartTracking(agent) => {
      val agentTracker = createTracker(agent)
      trackerState(agentTracker) = Unavailable
      trackerActors(AgentGateway.address(agent)) = agentTracker
      agentTracker ! SubscribeTransitionCallBack(self)
    }

    case StopTracking(agent) => {
      val tracker = lookupTracker(agent)
      context.stop(tracker)
      trackerState -= tracker
      trackerActors -= AgentGateway.address(agent)
      trackerStats -= tracker
    }

    case (a: RemoteAgent, stat: JobsStat) => {
       trackerStats(sender) = JobStats(stat.activeJobs, stat.totalJobs)
    }
  }

  def lookupTracker(agent: RemoteAgent): ActorRef = lookupTracker(AgentGateway.address(agent))

  def lookupTracker(address: Address): ActorRef = trackerActors.getOrElse(address, context.system.deadLetters) //todo: not existing address
}

class StatusTracker(agent: RemoteAgent, pollingPeriodSecs: Int) extends Actor with FSM[AgentStatus, Data] {

  private val timer = "remote-actor-status-check"
  private val remoteAgent = AgentGateway.resolve(agent)

  startWith(if (remoteAgent != context.system.deadLetters) Disconnected else Error, Uninitialized)

  when(Disconnected) {
    case Event(_: RemoteClientConnected, _) => goto(Connected)
  }

  when(Connected) {
    case Event(e: StatusResponse, _) => goto(Active).using(JobsStat(e.totalJobs, e.activeJobs))
  }

  onTransition {
    case api.AgentStatus.Disconnected -> api.AgentStatus.Connected  => remoteAgent ! GetStatus
  }

  when(Active, stateTimeout = (pollingPeriodSecs * 3) seconds) {
    case Event(e: StatusResponse, _) =>
      val stat: JobsStat = JobsStat(e.totalJobs, e.activeJobs)
      context.parent ! (agent, stat)
      stay().using(stat)
    case Event(StateTimeout, _) => goto(Disconnected)
  }

  whenUnhandled {
    case Event(e: StatusResponse, _) => goto(Active).using(JobsStat(e.totalJobs, e.activeJobs))
    case Event(e: RemoteClientDisconnected, _) => goto(Disconnected)
    case Event(GetStatus, _) => {
      remoteAgent ! GetStatus
      stay()
    }
  }

  override def preStart() {
    if(stateName != Error) {
      setTimer(timer, GetStatus, pollingPeriodSecs seconds, repeat = true)
      remoteAgent ! GetStatus
    }
    super.preStart()
  }

  override def postStop() {
    cancelTimer(timer)
    super.postStop()
  }
}


sealed trait Data
case object Uninitialized extends Data
case class JobsStat (totalJobs: Int, activeJobs: Int) extends Data
