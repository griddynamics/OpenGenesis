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
package com.griddynamics.genesis.build

import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.plugin.{PartialStepCoordinatorFactory, GenesisStepResult, StepExecutionContext}

class BuildStepCoordinatorFactory(pluginContext : () => BuildContext) extends PartialStepCoordinatorFactory {
    def apply(step: Step, context: StepExecutionContext) = new BuildStepCoordinator(step, context, pluginContext())
    def isDefinedAt(step: Step) = step.isInstanceOf[BuildStep]
}

class BuildStepCoordinator(val step : Step, context: StepExecutionContext, pluginContext : BuildContext) extends StepCoordinator
    with Logging{

    var stepFailed = false

    def onStepStart() = {
        val buildStep: BuildStep = step.asInstanceOf[BuildStep]

        pluginContext.buildProvider(buildStep.provider) match {
            case None => {
                log.debug("No build provider found for name = [%s]", buildStep.provider)
                stepFailed = true;
                Seq()
            }
            case Some(provider) =>
              Seq(new BuildActionExecutor(new BuildAction(buildStep), provider))
        }
    }

    def onStepInterrupt(signal: Signal) = {
      stepFailed = true
      Seq()
    }

    def onActionFinish(result: ActionResult) = {
        result match {
            case result : BuildSuccessful => {
                context.globals ++= result.outResult
                stepFailed = false
            }
            case _ => {
                stepFailed = true
            }
        }
        Seq()
    }

    def getStepResult() = {
        new GenesisStepResult(isStepFailed = stepFailed, step = context.step,
            envUpdate = context.envUpdate(), serversUpdate = context.serversUpdate())
    }
}

class BuildActionExecutor(val action : BuildAction, provider : BuildProvider) extends SimpleAsyncActionExecutor with Logging {
  def startAsync() {
        log.debug("Starting build")
        provider.build(action.step.values)
    }

  def getResult() = {
    val query: Option[BuildResult] = provider.query()
    log.debug("Intermediate result is: %s".format(query))
    query match {
      case None => None
      case Some(x) => {
        if (x.success) 
          Some(BuildSuccessful(action, x.results))
        else
          Some(BuildFailed(action))
      }
    }
  }

  override def cleanUp(signal: Signal) {
     provider.cancel()
  }
}

case class BuildAction(step : BuildStep) extends Action
case class BuildSuccessful(action: Action, outResult : Map[String, String]) extends ActionResult
case class BuildFailed(action: Action) extends ActionResult with ActionFailed