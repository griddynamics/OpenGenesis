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
package com.griddynamics.genesis.plugin

import collection.mutable
import com.griddynamics.genesis.model._
import scala.Some

trait StepExecutionContext {
    def step : GenesisStep

    def env : Environment
    def servers : Seq[EnvResource]

    def virtualMachines: Seq[VirtualMachine]

    def workflow : Workflow

    def servers(roleStep : RoleStep) : Seq[EnvResource]

    def updateEnv(env : Environment)
    def updateServer(server : EnvResource)

    def envUpdate() : Option[Environment]
    def serversUpdate() : Seq[EnvResource]
    def globals : mutable.Map[String, AnyRef]
    def pluginContexts: mutable.Map[String, Any]
}

class StepExecutionContextImpl(val step : GenesisStep,
                               iEnv : Environment,
                               iVms : Seq[EnvResource],
                               iWorkflow : Workflow,
                               val globals : mutable.Map[String, AnyRef],
                               val pluginContexts: mutable.Map[String, Any]) extends StepExecutionContext {
    var hEnv = iEnv
    var hVms = mutable.Seq(iVms : _ *)

    var envUpdated = false
    val vmsUpdates = mutable.Set[GenesisEntity.Id]()

    def env = hEnv.copy()
    def servers = hVms.map(_.copy()).toSeq

    def workflow = iWorkflow.copy()

    def servers(roleStep : RoleStep) =
        for (vm <- hVms.toSeq if vm.workflowId == workflow.id || roleStep.isGlobal
                              if roleStep.roles.contains(vm.roleName))
            yield vm.copy()

    def updateEnv(env: Environment) {
        envUpdated = true
        hEnv = env
    }

    def updateServer(server: EnvResource) {
        val index = hVms.indexWhere(vm => vm.id == server.id && vm.getClass == server.getClass)

        if (index == -1)
            hVms = hVms :+ server
        else
            hVms(index) = server

        vmsUpdates += hVms.indexWhere(vm => vm.id == server.id && vm.getClass == server.getClass)
    }

    def envUpdate() = if (envUpdated) Some(hEnv) else None

    def serversUpdate() = vmsUpdates.toSeq.map(hVms(_))

    def virtualMachines = servers.collect { case vm: VirtualMachine => vm }
}