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
package com.griddynamics.genesis.plugin

import collection.mutable
import com.griddynamics.genesis.model.{GenesisEntity, Workflow, VirtualMachine, Environment}

trait StepExecutionContext {
    def step : GenesisStep

    def env : Environment
    def vms : Seq[VirtualMachine]

    def workflow : Workflow

    def vms(roleStep : RoleStep) : Seq[VirtualMachine]

    def updateEnv(env : Environment)
    def updateVm(vm : VirtualMachine)

    def envUpdate() : Option[Environment]
    def vmsUpdate() : Seq[VirtualMachine]
    def globals : mutable.Map[String,String]
    def pluginContexts: mutable.Map[String, Any]
}

class StepExecutionContextImpl(val step : GenesisStep,
                               iEnv : Environment,
                               iVms : Seq[VirtualMachine],
                               iWorkflow : Workflow,
                               val globals : mutable.Map[String,String],
                               val pluginContexts: mutable.Map[String, Any]) extends StepExecutionContext {
    var hEnv = iEnv
    var hVms = mutable.Seq(iVms : _ *)

    var envUpdated = false
    val vmsUpdates = mutable.Set[GenesisEntity.Id]()

    def env = hEnv.copy()
    def vms = hVms.map(_.copy()).toSeq

    def workflow = iWorkflow.copy()

    def vms(roleStep : RoleStep) =
        for (vm <- hVms.toSeq if vm.workflowId == workflow.id || roleStep.isGlobal
                              if roleStep.roles.contains(vm.roleName))
            yield vm.copy()

    def updateEnv(env: Environment) {
        envUpdated = true
        hEnv = env
    }

    def updateVm(vm: VirtualMachine) {
        val index = hVms.indexWhere(_.id == vm.id)

        if (index == -1)
            hVms = hVms :+ vm
        else
            hVms(index) = vm

        vmsUpdates += hVms.indexWhere(_.id == vm.id)
    }

    def envUpdate() = if (envUpdated) Some(hEnv) else None

    def vmsUpdate() = vmsUpdates.toSeq.map(hVms(_))
}