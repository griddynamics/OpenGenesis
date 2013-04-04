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
package com.griddynamics.genesis.core

import com.griddynamics.genesis.plugin._
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.model.ActionTrackingStatus

class TrivialStepCoordinatorFactory(executor: TrivialStepExecutor[_ <: Step, _ <: StepResult]) extends PartialStepCoordinatorFactory {

  def apply(step: Step, context: StepExecutionContext): StepCoordinator = {
    new InternalSingleStepCoordinator(executor.asInstanceOf[TrivialStepExecutor[Step, StepResult]], step, context)
  }

  def isDefinedAt(step: Step) = executor.stepType.isAssignableFrom(step.getClass)

}


private class InternalSingleStepCoordinator(executor: TrivialStepExecutor[Step, StepResult], sstep: Step, context: StepExecutionContext) extends StepCoordinator {
  var stepResult: StepResult = _

  def onActionFinish(result: ActionResult) = {
    result match {
      case r: InternalActionResult => stepResult = r.stepResult
      case et: ActionFailed => stepResult = new FailResult {
        val step = sstep
      }
      case _ =>
    }
    Seq()
  }

  def onStepInterrupt(signal: Signal) = Seq()

  def onStepStart() = {
    val internalAction = new InternalAction(step)

    Seq(new SyncActionExecutor {
      def startSync() = {
        try {
          new InternalActionResult(action, executor.execute(sstep, context))
        } catch {
          case e: StepExecutionException => new ActionFailed {
            val action = internalAction
            override val desc = e.msg
          }
        }
      }

      def cleanUp(signal: Signal) {}

      val action = internalAction
    })
  }

  def getStepResult() = stepResult

  def step = sstep
}


private class InternalAction(val step: Step) extends Action {
  override def desc = "Step execution"
}

private class InternalActionResult(val action: Action, val stepResult: StepResult) extends ActionResult {
  override def outcome = {
    stepResult match {
      case f: FallibleResult if f.isStepFailed => ActionTrackingStatus.Failed
      case _ => ActionTrackingStatus.Succeed
    }
  }

  override def desc = ""
}