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
package com.griddynamics.genesis.chef.coordinator

import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.chef.step.ChefRun
import com.griddynamics.genesis.chef.action._
import com.griddynamics.genesis.exec.action._
import com.griddynamics.genesis.exec._
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}
import com.griddynamics.genesis.model.VmStatus
import com.griddynamics.genesis.logging.InternalLogger
import com.griddynamics.genesis.chef.{ChefVmAttrs, ChefPluginContext}
import com.griddynamics.genesis.actions.json.{PreprocessingJsonAction, PreprocessingSuccess}
import net.liftweb.json.JsonAST.{JObject, JField}
import net.liftweb.json.JsonParser

class ChefRunCoordinator(val step: ChefRun,
                         stepContext: StepExecutionContext,
                         execPluginContext: ExecPluginContext,
                         chefPluginContext: ChefPluginContext) extends ActionOrientedStepCoordinator with InternalLogger {
  override val stepId = stepContext.step.id
  var isStepFailed = false

  def onStepStart() = {
    writeLog("Starting phase %s".format(stepContext.step.phase))
    val (tVms, clearVms) = stepContext.servers(step).filter(_.isReady).partition(_.get(ExecVmAttrs.HomeDir).isDefined)
    val (chefVms, execVms) = tVms.partition(_.get(ChefVmAttrs.ChefNodeName).isDefined)

    val clearActions = clearVms.map(InitExecNode(stepContext.env, _))
    val execActions = execVms.map(InitChefNode(stepContext.env, _))
    val jsonActions = chefVms.map(m => PreprocessingJsonAction(stepContext.env, m, Map(), Map(), step.jattrs, step.templates, m.roleName))
    val chefActions = chefVms.map(PrepareRegularChefRun(stepContext.step.id.toString, stepContext.env,
      _, step.runList, step.jattrs))

    clearActions ++ execActions ++ jsonActions ++ chefActions
  }

  def onActionFinish(result: ActionResult) = {
    writeLog("Action finished with result %s".format(result.getClass.getSimpleName))
    result match {
      case _ if isStepFailed => {
        Seq()
      }
      case a@ExecFinished(_, _) if (!a.isExecSuccess) => {
        isStepFailed = true
        Seq()
      }
      case ExecInitFail(a) => {
        stepContext.updateServer(a.server)
        isStepFailed = true
        Seq()
      }

      case ExecInitSuccess(a) => {
        stepContext.updateServer(a.server)
        Seq(InitChefNode(a.env, a.server))
      }
      case ChefInitSuccess(a, d) => {
        stepContext.updateServer(a.server)
        Seq(RunPreparedExec(d, a))
      }
      case ChefRunPrepared(a, d) => {
        Seq(RunPreparedExec(d, a))
      }
      case ExecFinished(RunPreparedExec(details, _: InitChefNode), _) => {
        Seq(PrepareInitialChefRun(details.env, details.server))
      }
      case ExecFinished(RunPreparedExec(details, _: PrepareInitialChefRun), _) => {
        Seq(PreprocessingJsonAction(details.env, details.server, Map(), Map(), step.jattrs, step.templates, details.server.roleName))
      }

      case PreprocessingSuccess(action, server, json)  => {
          val label = "%d.%d.%s".format(stepContext.workflow.id, stepContext.step.id,
              stepContext.step.phase)
          Seq(PrepareRegularChefRun(label, action.env, action.server, step.runList, JsonParser.parse(json).asInstanceOf[JObject]))
      }

      case ExecFinished(RunPreparedExec(_, _: PrepareRegularChefRun), _) => {
        Seq()
      }
      case _ => {
        isStepFailed = true
        Seq()
      }
    }
  }

  def getStepResult() = {
    GenesisStepResult(stepContext.step,
      isStepFailed = isStepFailed,
      envUpdate = stepContext.envUpdate(),
      serversUpdate = stepContext.serversUpdate())
  }

  def getActionExecutor(action: Action) = {
    writeLog("Starting action %s".format(action.getClass.getSimpleName))
    action match {
      case a: InitExecNode => execPluginContext.execNodeInitializer(a)
      case a: InitChefNode => chefPluginContext.chefNodeInitializer(a)
      case a: PrepareInitialChefRun => chefPluginContext.initialChefRunPreparer(a)
      case a: PreprocessingJsonAction => chefPluginContext.preprocessJsonAction(a)
      case a: PrepareRegularChefRun => chefPluginContext.regularChefRunPreparer(a)
      case a: RunExec => execPluginContext.execRunner(a)
    }
  }

  def onStepInterrupt(signal: Signal) = Seq()
}
