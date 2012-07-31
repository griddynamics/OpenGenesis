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
package com.griddynamics.genesis.jclouds.coordinators

import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.jclouds.step.{ProvisionVm => ProvisionVmStep}
import com.griddynamics.genesis.model.VmStatus
import com.griddynamics.coordinators.provision.AbstractProvisionVmsStepCoordinator
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.jclouds.action.JCloudsProvisionVm
import com.griddynamics.genesis.jclouds.{Account, Plugin, JCloudsProvisionContext}
import com.griddynamics.genesis.service.{CredentialService, CredentialsStoreService}
import com.griddynamics.genesis.workflow.Action

class ProvisionVmsStepCoordinator(override val step: ProvisionVmStep,
                                  override val context: StepExecutionContext,
                                  override val pluginContext: JCloudsProvisionContext,
                                  credStore: CredentialsStoreService,
                                  credService: CredentialService) extends AbstractProvisionVmsStepCoordinator[JCloudsProvisionVm] {

  def onStepStart(): Seq[Action] = {
    LoggerWrapper.writeLog(context.step.id, "Starting phase %s".format(context.step.phase))
    val existingVms = context.virtualMachines.filter(vm => vm.stepId == context.step.id && vm.status == VmStatus.Ready)

    if(!step.account.isEmpty && !Account.isValid(step.account)) {
      LoggerWrapper.writeLog(context.step.id,
        "Invalid 'account' properties were supplied to provision step. Provisioning aborted.")
      this.stepFailed = true
      return Seq()
    }

    val computeSettings = if (step.account.isEmpty) pluginContext.computeSettings else Account.mapToComputeSettings(step.account)
    val provider = computeSettings(Plugin.Provider).toString

    val credentials = step.keyPair.flatMap { credStore.find(context.env.projectId, provider, _) }

    if(credentials.isDefined || credService.defaultCredentials.isDefined) {
      for (n <- 1 to (step.quantity - existingVms.size)) yield {
        JCloudsProvisionVm(context.env,
          context.workflow,
          context.step,
          step.roleName,
          step.hardwareId,
          step.imageId,
          step.instanceId,
          step.ip,
          Some(provider),
          step.keyPair,
          step.securityGroup,
          computeSettings
        )
      }
    } else {
      LoggerWrapper.writeLog(context.step.id,
        "Failed to find credentials '%s' for cloud provider '%s' in credentials store. No default credentials installed. ".format(step.keyPair.get, provider))
      LoggerWrapper.writeLog(context.step.id, "Provisioning aborted.")
      this.stepFailed = true
      Seq()
    }
  }
}