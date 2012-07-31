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
package com.griddynamics.genesis.actions.provision

import com.griddynamics.genesis.workflow.{ActionFailed, Action, ActionResult}
import com.griddynamics.genesis.model.{Workflow, VirtualMachine, Environment}
import com.griddynamics.genesis.plugin.GenesisStep


trait ProvisionAction extends Action

trait SpecificProvisionVmAction extends Action {
  def env : Environment
  def ip : Option[String]
  def workflow : Workflow
  def roleName : String
  def instanceId : Option[String]
  def step : GenesisStep

  def newVm : VirtualMachine
}

case class CheckSshPortAction(env: Environment, vm: VirtualMachine) extends ProvisionAction

case class DestroyVmAction(vm: VirtualMachine) extends ProvisionAction

case class CheckPortAction(vm: VirtualMachine, port: Int) extends ProvisionAction

case class CheckPublicIpAction(vm: VirtualMachine) extends ProvisionAction

sealed trait ProvisionResult extends ActionResult

case class ProvisionCompleted(action: SpecificProvisionVmAction,
                              vm: VirtualMachine) extends ProvisionResult

case class ProvisionFailed(action: SpecificProvisionVmAction,
                           vm: Option[VirtualMachine],
                           timedOut: Boolean) extends ProvisionResult with ActionFailed {
  override def desc = if (timedOut) {
    "Provisioning timed out"
  } else {
    "Failed to complete provisioning"
  }
}


case class PublicIpCheckCompleted(action: CheckPublicIpAction) extends ProvisionResult

case class PublicIpCheckFailed(action: CheckPublicIpAction, vm: VirtualMachine) extends ProvisionResult with ActionFailed

case class SshCheckCompleted(action: CheckSshPortAction, vm: VirtualMachine) extends ProvisionResult

case class SshCheckFailed(action: CheckSshPortAction, vm: VirtualMachine) extends ProvisionResult with ActionFailed

case class NoCredentialsFound(action: CheckSshPortAction, vm: VirtualMachine) extends ProvisionResult with ActionFailed

case class VmDestroyed(action: DestroyVmAction, vm: VirtualMachine) extends ProvisionResult

case class PortTestCompleted(action: CheckPortAction) extends ProvisionResult

case class PortTestFailed(action: CheckPortAction, vm: VirtualMachine) extends ProvisionResult with ActionFailed