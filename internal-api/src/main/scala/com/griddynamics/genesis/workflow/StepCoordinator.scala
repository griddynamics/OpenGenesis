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

import com.griddynamics.genesis.workflow.step.{ActionStepResult, ActionStep}

/* Trait for classes responsible for coordination of step
 * execution process. Main life cycle involves creating
 * ActionExecutor's in response for receiving results of
 * previous actions
 */
trait StepCoordinator {
    def step: Step

    /* Retrieve a set of action executors which will be activated first */
    def onStepStart(): Seq[ActionExecutor]

    /* Retrieve set of action executors which will be after interrupt signal */
    def onStepInterrupt(signal: Signal): Seq[ActionExecutor]

    /* Retrieve set of action executors which must be started after result reception */
    def onActionFinish(result: ActionResult): Seq[ActionExecutor]

    /* Retrieve step result. Called when all started action executors was
     * finished and no more executors was started
     */
    def getStepResult(): StepResult
}

/* Analog of step coordinator oriented on action passing instead of
 * ActionExecutor. All methods have same meaning as in StepCoordinator
 * trait. Additional factory method presented for creating ActionExecutor
 * by provided action to simplify event loop programming.
 */
trait ActionOrientedStepCoordinator {
    def step: Step

    /* Same as in StepCoordinator */
    def onStepStart(): Seq[Action]

    /* Same as in StepCoordinator */
    def onStepInterrupt(signal: Signal): Seq[Action]

    /* Same as in StepCoordinator */
    def onActionFinish(result: ActionResult): Seq[Action]

    /* Same as in StepCoordinator */
    def getStepResult(): StepResult

    /* Factory method for creating ActionExecutor by action */
    def getActionExecutor(action: Action): ActionExecutor
}

object ActionOrientedStepCoordinator {
    implicit def toStepCoordinator(delegate: ActionOrientedStepCoordinator) = new StepCoordinator {
        val step = delegate.step

        def onStepStart() =
            delegate.onStepStart().map(delegate.getActionExecutor(_))

        def onStepInterrupt(signal: Signal) =
            delegate.onStepInterrupt(signal).map(delegate.getActionExecutor(_))

        def onActionFinish(result: ActionResult) =
            delegate.onActionFinish(result).map(delegate.getActionExecutor(_))

        def getStepResult() = delegate.getStepResult()
    }
}

class ActionStepCoordinator(actionExecutor: ActionExecutor) extends StepCoordinator {
    val step = ActionStep(actionExecutor.action)

    var actionResult: ActionResult = _

    def onStepStart() = Seq(actionExecutor)

    def onStepInterrupt(signal: Signal) = Seq()

    def onActionFinish(result: ActionResult) = {
        actionResult = result
        Seq()
    }

    def getStepResult() = ActionStepResult(step, actionResult)
}
