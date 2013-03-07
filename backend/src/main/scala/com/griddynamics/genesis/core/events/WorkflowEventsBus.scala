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
 */ package com.griddynamics.genesis.core.events

import com.griddynamics.genesis.model.WorkflowStatus

object WorkflowEventsBus {

  private val subscriptions = new java.util.concurrent.CopyOnWriteArrayList[PartialFunction[WorkflowEvent, Unit]]

  private val wrapper: Iterable[PartialFunction[WorkflowEvent, Unit]] = {
    import scala.collection.JavaConversions._
    subscriptions
  }

  def subscribe(sub: PartialFunction[WorkflowEvent, Unit]) { subscriptions.add(sub) }
  def removeSubscriptions(sub: PartialFunction[WorkflowEvent, Unit]) { subscriptions.remove(sub) }
  def removeSubscriptions() { subscriptions.clear() }

  def publish(event: WorkflowEvent) {
    wrapper.foreach { k => if (k.isDefinedAt (event)) k(event) }
  }
}


trait WorkflowEventsSubscriber extends PartialFunction[WorkflowEvent, Unit]

sealed trait WorkflowEvent

case class WorkflowFinished(projectId: Int, envId: Int, workflowId: Int, status: WorkflowStatus.WorkflowStatus) extends WorkflowEvent
case class EnvDestroyed(projectId: Int, envId: Int) extends WorkflowEvent