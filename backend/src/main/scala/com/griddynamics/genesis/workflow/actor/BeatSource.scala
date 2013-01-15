/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.workflow.actor

import com.griddynamics.genesis.workflow.message._
import akka.actor.{TypedActor, ActorRef}
import java.util.concurrent.TimeUnit
import collection.mutable
import com.griddynamics.genesis.util.Logging
import scala.concurrent.duration._
import concurrent.ExecutionContext

trait BeatSource {
  def start()

  def beat()

  def finish()

  def tryToFinish()

  def subscribe(actorRef: ActorRef, beat: Beat)

  def unsubscribe(actorRef: ActorRef, unsubscribeCallback: => Unit)

  def subscribeOnce(actorRef: ActorRef, beat: Beat, beatDelayMs: Long)

  def unsubscribeOnce(actorRef: ActorRef, unsubscribeCallback: => Unit)
}

class BeatSourceImpl(beatPeriodMs: Long) extends BeatSource with Logging {
  val subscribeMap: mutable.Map[ActorRef, Beat] = mutable.WeakHashMap()
  var subscribeOnceMap: mutable.Map[ActorRef, (Beat, Long)] = mutable.WeakHashMap()
  def start() {
    val duration: FiniteDuration = Duration(beatPeriodMs, TimeUnit.MILLISECONDS)
    val scheduler = TypedActor.context.system.scheduler
    val receiver = TypedActor.self[BeatSource]
    implicit val ec: ExecutionContext = TypedActor.context.dispatcher
    scheduler.schedule(duration, duration, new Runnable {
      def run() {
         receiver.beat()
      }
    })
  }

  def beat() {
    notifySubscribers()
    notifyOnceSubscribers()
  }

  def finish() {
    TypedActor.context.stop(TypedActor.context.self)
  }

  def tryToFinish() {
    if (subscribeMap.isEmpty && subscribeOnceMap.isEmpty)
      finish()
  }

  def subscribe(actorRef: ActorRef, beat: Beat) {
    log.debug("Subcribing %s", actorRef)
    subscribeMap(actorRef) = beat
  }

  def unsubscribe(actorRef: ActorRef, unsubscribeCallback: => Unit) {
    log.debug("Unsubscribing %s", actorRef)
    subscribeMap -= actorRef
    unsubscribeCallback
  }

  def notifySubscribers() {
    for ((subscriber, beat) <- subscribeMap)
      subscriber ! beat
  }

  def subscribeOnce(actorRef: ActorRef, beat: Beat, beatDelayMs: Long) {
    subscribeOnceMap(actorRef) = (beat, beatDelayMs)
  }

  def unsubscribeOnce(actorRef: ActorRef, unsubscribeCallback: => Unit) {
    subscribeOnceMap -= actorRef
    unsubscribeCallback
  }

  def notifyOnceSubscribers() {
    subscribeOnceMap.transform {
      case (a, (b, t)) => (b, t - beatPeriodMs)
    }

    subscribeOnceMap.filter {
      case (a, (b, t)) => t <= 0
    }
      .foreach {
      case (a, (b, t)) => a ! b
    }

    subscribeOnceMap.retain {
      case (a, (b, t)) => t > 0
    }
  }
}
