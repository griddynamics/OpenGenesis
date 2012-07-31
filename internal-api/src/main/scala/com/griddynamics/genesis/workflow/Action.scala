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
package com.griddynamics.genesis.workflow

import java.util
import com.griddynamics.genesis.model.ActionTrackingStatus

/* Marker trait for any particular action */
trait Action {
    def desc = DefaultDescription.toString(this)

    final val uuid = util.UUID.randomUUID().toString
}

/* Base trait for result of particular action */
trait ActionResult {
    def action: Action

    def desc = DefaultDescription.toString(this)

    def outcome: ActionTrackingStatus.ActionStatus = ActionTrackingStatus.Succeed
}

object DefaultDescription {
  def toString(obj: AnyRef) = obj.getClass.getSimpleName.replaceAll(
    String.format("%s|%s|%s", // PascalCaseName  -> Pascal Case Name
      "(?<=[A-Z])(?=[A-Z][a-z])",
      "(?<=[^A-Z])(?=[A-Z])",
      "(?<=[A-Za-z])(?=[^A-Za-z])"
    ),
    " ")
}

trait ActionFailed extends ActionResult {
    override def outcome = ActionTrackingStatus.Failed
}

trait ActionInterrupted extends ActionResult {
    override def outcome = ActionTrackingStatus.Interrupted
}

package action {
/* Result of action which executor has thrown an exception */
case class ExecutorThrowable private[workflow](action: Action, throwable: Throwable) extends ActionResult with ActionFailed

/* Result of action which wasn't finished because of executor interrupt */
case class ExecutorInterrupt private[workflow](action: Action, signal: Signal) extends ActionResult with ActionInterrupted
  
case class DelayedExecutorInterrupt private[workflow](action: Action, result : ActionResult, signal : Signal) extends ActionResult with ActionInterrupted
}
