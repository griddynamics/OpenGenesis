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
package com.griddynamics.genesis.chef

package action

import net.liftweb.json.JsonAST.JObject
import com.griddynamics.genesis.workflow.Action
import com.griddynamics.genesis.exec.ExecDetails
import com.griddynamics.genesis.workflow.ActionResult
import com.griddynamics.genesis.model.{EnvResource, Environment}

sealed trait ChefAction extends Action

case class InitChefNode(env: Environment, server: EnvResource) extends ChefAction

sealed trait PrepareChefRun extends ChefAction {
    def label: String
    def env: Environment
    def server: EnvResource
}

case class PrepareRegularChefRun(label: String,
                                 env: Environment,
                                 server: EnvResource,
                                 runList: Seq[String],
                                 jattrs: JObject) extends PrepareChefRun

case class PrepareInitialChefRun(env: Environment, server: EnvResource) extends PrepareChefRun {
    val label = "chef-init"
}

case class CreateChefDatabag(env: Environment,
                             databag: String,
                             items: Map[String, JObject],
                             overwrite : Boolean) extends ChefAction

case class CreateChefRole(env: Environment,
                          role: String,
                          description: String,
                          runList: Seq[String],
                          defaults: JObject,
                          overrides: JObject,
                          overwrite : Boolean) extends ChefAction

case class DestroyChefEnv(env: Environment) extends ChefAction

/* ------------------------------------- Results ------------------------------------- */

sealed trait ChefResult extends ActionResult

case class ChefInitSuccess(action: InitChefNode, execDetails: ExecDetails) extends ChefResult

case class ChefRunPrepared(action: PrepareChefRun, execDetails: ExecDetails) extends ChefResult

case class ChefDatabagCreated(action: CreateChefDatabag) extends ChefResult

case class ChefRoleCreated(action: CreateChefRole) extends ChefResult

case class ChefEnvDestroyed(action: DestroyChefEnv) extends ChefResult
