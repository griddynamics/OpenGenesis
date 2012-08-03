/*
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
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.chefsolo.coordinator

import com.griddynamics.genesis.workflow.{Signal, ActionResult, Action, ActionOrientedStepCoordinator}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.chefsolo.step.ChefsoloRunStep
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}
import com.griddynamics.genesis.chefsolo.action._
import com.griddynamics.genesis.model.{BorrowedMachine, VirtualMachine}
import com.griddynamics.genesis.logging.InternalLogger
import com.griddynamics.genesis.exec.action.{RunPreparedExec, ExecInitSuccess, InitExecNode}
import com.griddynamics.genesis.chefsolo.context.ChefSoloPluginContext
import com.griddynamics.genesis.actions.json.{PreprocessingSuccess, PreprocessingJsonAction}

class ChefsoloRunCoordinator(val step: ChefsoloRunStep,
                             stepContext : StepExecutionContext,
                             pluginContext: ChefSoloPluginContext) extends ActionOrientedStepCoordinator with Logging with InternalLogger {
  override val stepId = stepContext.step.id
  var isStepFailed = false

  def onStepStart() = {
      writeLog("Starting phase %s for role %s.".format(stepContext.step.phase, step.roles))
      val machines = stepContext.servers(step).filter(s => s.isReady)
      machines.map( InitExecNode(stepContext.env, _) )
  }

  def onStepInterrupt(signal: Signal) = {
      isStepFailed = true
      Seq()
  }

  def patternSubst : Map[String, String] = {
    if (step.dependsOn != null)
      step.dependsOn.flatMap(role => {
        stepContext.servers.filter(_.roleName == role).map { server =>

          val ip = server match {
            case vm: VirtualMachine => pluginContext.computeService.getIpAddresses(vm).get.publicIp.get
            case bm: BorrowedMachine => bm.getIp.get.address
          }

          (server.roleName.toUpperCase + "_HOST", ip)
        }
      }).toMap
    else
      Map()
  }

  def onActionFinish(result: ActionResult) = {
    writeLog("Action finished with result: %s".format(result))
    result match {

        case ExecInitSuccess(a) => {
            stepContext.updateServer(a.server)
            Seq(PrepareSoloAction(stepContext.env, a.server))
        }

        case SoloInitSuccess(action, details) => {
            log.debug("Details for next run are: %s", details)
            Seq(RunPreparedExec(details, action))
        }

        case e@ExtendedExecFinished(RunPreparedExec(_, a: PrepareSoloAction), _, _, _) =>  {
            if (e.isExecSuccess) {
                stepContext.updateServer(a.server)
                Seq(PreprocessingJsonAction(stepContext.env, a.server, patternSubst, Map(), step.jattrs, step.templateUrl, a.server.roleName))
            } else {
                isStepFailed = true
                Seq()
            }
        }

        case PreprocessingSuccess(_, vm, json) => {
            stepContext.updateServer(vm)
            Seq(PrepareNodeAction(stepContext.env, vm, json, "%s.%s".format(stepContext.workflow.id, stepContext.step.id), step.cookbookUrl))
        }

        case NodePrepared(a, vm, details) => {
            stepContext.updateServer(vm)
            Seq(RunPreparedExec(details, a))
        }

        case e@ExtendedExecFinished(RunPreparedExec(_, a: PrepareNodeAction), _, _, _)=> {
            isStepFailed = ! e.isExecSuccess
            Seq()
        }

        case _ => {
            isStepFailed = true
            Seq()
        }
    }
  }

  def getStepResult() = GenesisStepResult(stepContext.step,
    isStepFailed = isStepFailed,
    envUpdate = stepContext.envUpdate(),
    serversUpdate = stepContext.serversUpdate())

  def getActionExecutor(action: Action) = {
    writeLog("Starting action %s".format(action))
    action match {
      case init: InitExecNode => pluginContext.initExecNodeExecutor(init)
      case json: PreprocessingJsonAction => pluginContext.preprocessJsonExecution(json)
      case prepare: PrepareNodeAction => pluginContext.prepareChefSoloExecution(prepare)
      case prepared: PrepareSoloAction => pluginContext.initChefSoloExecution(prepared)
      case exec: RunPreparedExec => pluginContext.execExecutor(exec)
    }
  }
}
