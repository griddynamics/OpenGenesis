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

package com.griddynamics.genesis.workflow

import com.griddynamics.genesis.util.StringUtils

trait ActionWithDesc extends Action {
  def desc = DefaultDescription.toString(this)
}

trait ActionResultWithDesc extends ActionResult {
   def desc = DefaultDescription.toString(this)
}

object DefaultDescription {
  def toString(obj: AnyRef) = StringUtils.splitByCase(obj.getClass.getSimpleName)
}

package action {

/* Result of action which executor has thrown an exception */
case class ExecutorThrowable private[workflow](action: Action, throwable: Throwable) extends ActionResultWithDesc with ActionFailed

/* Result of action which wasn't finished because of executor interrupt */
case class ExecutorInterrupt private[workflow](action: Action, signal: Signal) extends ActionResultWithDesc with ActionInterrupted

case class DelayedExecutorInterrupt private[workflow](action: Action, result : ActionResult, signal : Signal) extends ActionResultWithDesc with ActionInterrupted
}