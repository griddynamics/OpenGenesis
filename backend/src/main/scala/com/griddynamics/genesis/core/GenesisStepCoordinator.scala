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
package com.griddynamics.genesis.core

import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.plugin._
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.model.{ActionTracking, Workflow}
import com.griddynamics.genesis.model.WorkflowStepStatus._
import scala.Some
import com.griddynamics.genesis.plugin.GenesisStepResult
import com.griddynamics.genesis.plugin.GenesisStep


class GenesisStepCoordinator(val step: GenesisStep,
                             workflow : Workflow,
                             stepCoordinator: StepCoordinator,
                             storeService : StoreService) extends StepCoordinator {
    def onStepStart() = {
        setStepStatus(Executing)
        trackStart(stepCoordinator.onStepStart())
    }

    def onActionFinish(result: ActionResult) = {
        storeService.endAction(result.action.uuid, Some(result.desc), result.outcome)
        trackStart(stepCoordinator.onActionFinish(result))
    }

    def getStepResult() = {
        val genesisStepResult = stepCoordinator.getStepResult() match {
            case result: GenesisStepResult => result
            case result => {
                var genesisResult = GenesisStepResult(step, actualResult = Some(result))

                if (result.isInstanceOf[FallibleResult])
                    genesisResult = genesisResult.copy(isStepFailed = result.asInstanceOf[FallibleResult].isStepFailed)

                if (result.isInstanceOf[EnvUpdateResult])
                    genesisResult = genesisResult.copy(envUpdate = result.asInstanceOf[EnvUpdateResult].envUpdate)

                if (result.isInstanceOf[ServersUpdateResult])
                    genesisResult = genesisResult.copy(serversUpdate = result.asInstanceOf[ServersUpdateResult].serversUpdate)

                genesisResult
            }
        }

        setStepDetailsAndStatus(if(genesisStepResult.isStepFailed) Failed else Succeed)
        genesisStepResult
    }


    def onStepInterrupt(signal: Signal) = {
        setStepStatus(Canceled)
        storeService.cancelRunningActions(step.id)
        trackStart(stepCoordinator.onStepInterrupt(signal))
    }

    private def setStepStatus(status : WorkflowStepStatus) {
        storeService.updateStepStatus(step.id, status)
    }

    private def setStepDetailsAndStatus(status : WorkflowStepStatus) {
        storeService.updateStepDetailsAndStatus(step.id, Some(step.actualStep.stepDescription), status)
    }

    private def trackStart(result: scala.Seq[ActionExecutor]) : Seq[ActionExecutor] = {
        result.map(a => {
            storeService.startAction(ActionTracking(step.id, a.action))
            a
        })
    }
}
