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
package com.griddynamics.genesis.run

import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext, PartialStepCoordinatorFactory}
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.util.Logging
import java.io.File
import com.griddynamics.genesis.logging.LoggerWrapper

class RunLocalStepCoordinator(stepContext: StepExecutionContext, val step: RunLocalStep, shellService: LocalShellExecutionService) extends ActionOrientedStepCoordinator with Logging {
  var isStepFailed = false

  var toExecute = new scala.collection.mutable.Queue[Action]()

  def onStepStart(): Seq[Action] = {
    step.output.foreach { directory =>
      if(!directory.exists() && !directory.mkdirs()) {
        LoggerWrapper.writeLog(stepContext.step.id, "Couldn't create directory [%s]".format(directory.getAbsolutePath))
        isStepFailed = true
        return Seq()
      }
    }

    val actions = step.commands.zipWithIndex.map { case (command, index) =>
      val outputDirectory = step.output.map(new File(_, index.toString))
      new RunLocalShell(step.shell, command, step.successExitCode, outputDirectory)
    }

    if(step.runInParallel) {
      actions
    } else {
      toExecute.enqueue(actions.toArray: _*)
      Seq(toExecute.dequeue())
    }
  }

  def onStepInterrupt(signal: Signal) = Seq()

  def onActionFinish(result: ActionResult) = result match {
    case _ if isStepFailed => {
      Seq()
    }
    case a: RunLocalResult => {
      isStepFailed = a.response.exitCode != step.successExitCode
      if(isStepFailed) {
        LoggerWrapper.writeLog(a.action.uuid, "FAILURE: Process finished with exit code %d, expected result = %d".format(a.response.exitCode, step.successExitCode))
      }
      if (!isStepFailed && !toExecute.isEmpty) {
        Seq(toExecute.dequeue())
      } else {
        Seq()
      }
    }
    case _ => {
      isStepFailed = true
      Seq()
    }
  }

  def getStepResult() = GenesisStepResult(stepContext.step,
    isStepFailed = isStepFailed,
    envUpdate = stepContext.envUpdate(),
    serversUpdate = stepContext.serversUpdate())

  def getActionExecutor(action: Action) = action match {
    case a: RunLocalShell => new RunLocalActionExecutor(a, stepContext.step.id, shellService)
  }
}

class RunLocalStepCoordinatorFactory(shellService: LocalShellExecutionService) extends PartialStepCoordinatorFactory {

  def isDefinedAt(step: Step) = step.isInstanceOf[RunLocalStep]

  def apply(step: Step, context: StepExecutionContext) = step match {
    case s: RunLocalStep => new RunLocalStepCoordinator(context, s, shellService)
  }
}
