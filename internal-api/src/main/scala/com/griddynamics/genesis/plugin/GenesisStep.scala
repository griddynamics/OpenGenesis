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

import com.griddynamics.genesis.workflow.{StepResult, Step}
import com.griddynamics.genesis.model.{EnvResource, GenesisEntity, Environment}

case class GenesisStep(id: GenesisEntity.Id,
                       phase: String,
                       precedingPhases: Set[String],
                       ignoreFail: Boolean,
                       retryCount: Int,
                       actualStep: Step,
                       exportTo: Map[String, String] = Map()) extends Step


case class GenesisStepResult(step: GenesisStep,
                             isStepFailed: Boolean = false,
                             envUpdate: Option[Environment] = None,
                             serversUpdate: Seq[EnvResource] = Seq(),
                             actualResult: Option[StepResult] = None) extends StepResult
with FallibleResult
with EnvUpdateResult
with ServersUpdateResult

trait FallibleResult extends StepResult {
    def isStepFailed: Boolean
}

trait FailResult extends FallibleResult {
    def isStepFailed = true
}

trait SuccessResult extends FallibleResult {
    def isStepFailed = false
}

trait RoleStep extends Step {
    def roles: Set[String]

    def isGlobal: Boolean
}

trait EnvUpdateResult extends StepResult {
    def envUpdate: Option[Environment]
}

trait ServersUpdateResult extends StepResult {
    def serversUpdate: Seq[EnvResource]
}
