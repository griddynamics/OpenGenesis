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
package com.griddynamics.genesis.exec

package action

import com.griddynamics.genesis.workflow.{ActionFailed, ActionResult, Action}
import com.griddynamics.genesis.model
import model.{Environment, EnvResource}

sealed trait ExecAction extends Action

case class InitExecNode(env: Environment, server: EnvResource) extends ExecAction

trait RunExec extends ExecAction {
    val execDetails: ExecDetails
}

case class RunCustomExec(execDetails: ExecDetails) extends RunExec

case class RunPreparedExec(execDetails: ExecDetails, prepareAction: Action) extends RunExec

case class RunExecWithArgs(execDetails: ExecDetails, args: String*) extends RunExec

case class UploadScripts(env:Environment, server: EnvResource, workingDir: String, script: String) extends ExecAction

/* ------------------------------------- Results ------------------------------------- */

trait ExecResult extends ActionResult

case class ExecInitSuccess(action: InitExecNode) extends ExecResult

case class ExecInitFail(action: InitExecNode) extends ExecResult with ActionFailed

case class ExecFinished(action: RunExec, val exitStatus: Option[Int]) extends ExecResult {
    def isExecSuccess = exitStatus.isDefined && exitStatus.get == 0
    override def outcome = if (isExecSuccess)
        model.ActionTrackingStatus.Succeed
    else
        model.ActionTrackingStatus.Failed
}


case class ScriptsUploaded(action: UploadScripts, server: EnvResource, outputPath: String, scriptPath: String) extends ExecResult
