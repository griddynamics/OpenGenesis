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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.jclouds.action

import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.actions.provision.SpecificProvisionVmAction
import com.griddynamics.genesis.model.{VmStatus, VirtualMachine, Workflow, Environment}


case class JCloudsProvisionVm(env: Environment,
                              workflow: Workflow,
                              step: GenesisStep,
                              roleName: String,
                              hardwareId: Option[String],
                              imageId: Option[String],
                              instanceId: Option[String] = None,
                              ip: Option[String] = None,
                              cloudProvider: Option[String] = None,
                              keyPair: Option[String] = None,
                              securityGroup: Option[String] = None,
                              provision: Map[String, Any] = Map()) extends SpecificProvisionVmAction {
  override val desc = "Provision VM Action"

  def newVm = {
    val vm = new VirtualMachine(
      envId = env.id,
      workflowId = workflow.id,
      stepId = step.id,
      status = VmStatus.Provision,
      roleName = roleName,
      instanceId = instanceId,
      hardwareId = hardwareId,
      imageId = imageId,
      cloudProvider = cloudProvider
    )
    vm.computeSettings = Option(provision)
    vm.keyPair = keyPair
    vm.securityGroup = securityGroup
    vm
  }
}