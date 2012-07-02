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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.jclouds.coordinators

import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.jclouds.step.{ProvisionVm => ProvisionVmStep}
import com.griddynamics.genesis.model.VmStatus
import com.griddynamics.coordinators.provision.AbstractProvisionVmsStepCoordinator
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.jclouds.action.JCloudsProvisionVm
import com.griddynamics.genesis.jclouds.JCloudsProvisionContext

class ProvisionVmsStepCoordinator(override val step: ProvisionVmStep,
                                  override val context: StepExecutionContext,
                                  override val pluginContext: JCloudsProvisionContext) extends AbstractProvisionVmsStepCoordinator[JCloudsProvisionVm] {

  def onStepStart() = {
    LoggerWrapper.writeLog(context.step.id, "Starting phase %s".format(context.step.phase))
    val existingVms = context.virtualMachines.filter(_.stepId == context.step.id)
      .filter(_.status == VmStatus.Ready)
    for (n <- 1 to (step.quantity - existingVms.size)) yield {
      JCloudsProvisionVm(context.env,
        context.workflow,
        context.step,
        step.roleName,
        step.hardwareId,
        step.imageId,
        step.instanceId,
        step.ip,
        Some(pluginContext.cloudProvider),
        step.keyPair,
        step.securityGroup,
        pluginContext.computeSettings
      )
    }
  }
}