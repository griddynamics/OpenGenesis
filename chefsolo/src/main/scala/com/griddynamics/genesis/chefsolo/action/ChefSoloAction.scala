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
package com.griddynamics.genesis.chefsolo.action

import com.griddynamics.genesis.model.{EnvResource, Environment}
import com.griddynamics.genesis.exec.ExecDetails
import net.liftweb.json.JsonAST.JObject
import com.griddynamics.genesis.exec.action.{ExecResult, RunExec}
import com.griddynamics.genesis.workflow.{Action, ActionResult}
import com.griddynamics.genesis.model

sealed trait ChefSoloAction extends Action

case class AddKeyAction(env: Environment, server: EnvResource) extends ChefSoloAction

case class PrepareNodeAction(env: Environment, server: EnvResource, json: String, label: String, cookbooksPath: String) extends ChefSoloAction

sealed trait ChefSoloActionResult extends ActionResult

case class NodePrepared(action: PrepareNodeAction, server: EnvResource, execDetails: ExecDetails) extends ChefSoloActionResult


sealed trait ExtendedExecResult extends ExecResult with ChefSoloActionResult {
    def exitStatus: Option[Int]
    def errLog: Option[String]
    def log: Option[String]
    def isExecSuccess = {
        exitStatus.isDefined && exitStatus.get == 0
    }
    override def outcome = if (isExecSuccess)
        model.ActionTrackingStatus.Succeed
    else
        model.ActionTrackingStatus.Failed
}
case class ExtendedExecFinished(action: RunExec, exitStatus: Option[Int],
                                errLog: Option[String], log: Option[String]) extends ExtendedExecResult

case class AddKeySuccess(action: AddKeyAction) extends ChefSoloActionResult

case class SoloInitSuccess(action: PrepareSoloAction, execDetails: ExecDetails) extends ChefSoloActionResult

case class PrepareSoloAction(env: Environment, server: EnvResource) extends ChefSoloAction