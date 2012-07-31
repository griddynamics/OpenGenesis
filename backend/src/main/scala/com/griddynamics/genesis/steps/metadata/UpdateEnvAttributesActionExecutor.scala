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
package com.griddynamics.genesis.metadata

import com.griddynamics.genesis.workflow.{ActionResult, Action, Signal, SyncActionExecutor}
import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.model.{Environment, DeploymentAttribute}
import com.griddynamics.genesis.service.StoreService

class UpdateEnvAttributesActionExecutor(val action: UpdateEnvAttributesAction, context: StepExecutionContext, storeService: StoreService) extends SyncActionExecutor {

  def cleanUp(signal: Signal) {}

  def startSync() = {
    val env = storeService.findEnv(action.env.id).get // updating optimistic lock counter
    val keys = action.entries.map(_.key).toSet

    val preserved = env.deploymentAttrs.filterNot{ attr => keys.contains(attr.key) }
    env.deploymentAttrs = preserved ++ action.entries

    context.updateEnv(env)
    storeService.updateEnv(env)

    SuccessfullyUpdated(action)
  }
}

case class SuccessfullyUpdated(val action: Action) extends ActionResult

case class UpdateEnvAttributesAction(env: Environment, entries: Seq[DeploymentAttribute]) extends Action