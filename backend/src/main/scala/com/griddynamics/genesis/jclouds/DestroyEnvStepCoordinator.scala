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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.jclouds

import com.griddynamics.genesis.workflow.{Action, ActionResult, Signal, ActionOrientedStepCoordinator}
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}
import com.griddynamics.genesis.workflow.action.{ExecutorInterrupt, ExecutorThrowable}
import com.griddynamics.genesis.jclouds.step.{DestroyEnv => DestroyEnvStep}
import com.griddynamics.genesis.actions.provision.{VmDestroyed, DestroyVmAction}

class DestroyEnvStepCoordinator(val step : DestroyEnvStep,
                                context : StepExecutionContext,
                                pluginContext: JCloudsPluginContext) extends ActionOrientedStepCoordinator {

    var stepFailed = false

    def getActionExecutor(action: Action) = {
        action match {
            case a : DestroyVmAction => pluginContext.destroyVmActionExecutor(a)
        }
    }

    def getStepResult() = {
        GenesisStepResult(context.step,
                          isStepFailed = stepFailed,
                          envUpdate = context.envUpdate(),
                          vmsUpdate = context.vmsUpdate())
    }

    def onActionFinish(result: ActionResult) = {
        result match {
            case VmDestroyed(_, vm) => {
                context.updateVm(vm)
                Seq()
            }
            case _ : ExecutorThrowable => {
                stepFailed = true
                Seq()
            }
            case _ : ExecutorInterrupt => {
                Seq()
            }
        }
    }

    def onStepInterrupt(signal: Signal) = Seq()

    def onStepStart() = for (vm <- context.vms) yield DestroyVmAction(vm)
}