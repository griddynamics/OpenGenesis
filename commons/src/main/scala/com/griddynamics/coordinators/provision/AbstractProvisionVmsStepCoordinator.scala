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
package com.griddynamics.coordinators.provision

import com.griddynamics.context.provision.ProvisionContext
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}
import com.griddynamics.genesis.workflow.action.{DelayedExecutorInterrupt, ExecutorInterrupt, ExecutorThrowable}
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.logging.LoggerWrapper


abstract class AbstractProvisionVmsStepCoordinator[A <: SpecificProvisionVmAction] extends ActionOrientedStepCoordinator {
  def pluginContext : ProvisionContext[A]
  def context : StepExecutionContext

  var stepFailed = false

  override def getActionExecutor(action: Action) : ActionExecutor = {
    LoggerWrapper.writeLog(context.step.id, "Starting action %s".format(action.getClass.getSimpleName))
    action match {
      case a: A => pluginContext.provisionVmActionExecutor(a)
      case a: CheckPublicIpAction => pluginContext.publicIpCheckActionExecutor(a)
      case a: CheckPortAction => pluginContext.portCheckActionExecutor(a)
      case a: CheckSshPortAction => pluginContext.sshPortCheckActionExecutor(a)
      case a: DestroyVmAction => pluginContext.destroyVmActionExecutor(a)
    }
  }

  override def getStepResult() = {
    LoggerWrapper.writeLog(context.step.id, "Phase %s finished with result %s".format(context.step.phase, stepFailed match {
      case true => "Fail"
      case false => "Success"
    }))
    GenesisStepResult(context.step,
      isStepFailed = stepFailed,
      envUpdate = context.envUpdate(),
      vmsUpdate = context.vmsUpdate())
  }

  override def onActionFinish(result: ActionResult) : Seq[Action] = {
    LoggerWrapper.writeLog(context.step.id, "Action %s finished with result %s".format(result.action.getClass.getSimpleName, result.getClass.getSimpleName))
    result match {
      case ProvisionCompleted(a, vm) => {
        context.updateVm(vm)
        Seq(CheckPublicIpAction(vm))
      }
      case ProvisionFailed(_, Some(vm)) => {
        context.updateVm(vm)
        stepFailed = true
        Seq(DestroyVmAction(vm))
      }
      case ProvisionFailed(_, None) => {
        stepFailed = true
        Seq()
      }
      case PortTestCompleted(a) => {
        Seq(CheckSshPortAction(context.env, a.vm))
      }
      case PortTestFailed(_, vm) => {
        context.updateVm(vm)
        stepFailed = true
        Seq(DestroyVmAction(vm))
      }
      case SshCheckCompleted(_, vm) => {
        context.updateVm(vm)
        Seq()
      }
      case SshCheckFailed(_, vm) => {
        context.updateVm(vm)
        stepFailed = true
        Seq(DestroyVmAction(vm))
      }
      case PublicIpCheckCompleted(a) => {
        Seq(CheckPortAction(a.vm, 22))
      }
      case PublicIpCheckFailed(_, vm) => {
        context.updateVm(vm)
        stepFailed = true
        Seq(DestroyVmAction(vm))
      }
      case VmDestroyed(_, vm) => {
        context.updateVm(vm)
        Seq()
      }
      case _ : ExecutorThrowable => {
        stepFailed = true
        Seq()
      }

      case interrupt : DelayedExecutorInterrupt => {
        interrupt.action match {
          case p : SpecificProvisionVmAction =>  {
            interrupt.result match {
              case ProvisionCompleted(_, vm) =>
              {
                context.updateVm(vm)
                Seq(DestroyVmAction(vm))
              }
              case _ => Seq()
            }
          }
          case _ => Seq()
        }
      }

      case _ : ExecutorInterrupt => {
        Seq()
      }
    }
  }

  override def onStepInterrupt(signal: Signal) = {
    for (vm <- context.vmsUpdate()) yield DestroyVmAction(vm)
  }
}