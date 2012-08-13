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
package com.griddynamics.genesis.service

import java.lang.RuntimeException
import com.griddynamics.genesis.common.Mistake
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.model.EnvStatus._
import com.griddynamics.genesis.model.WorkflowStepStatus._
import java.sql.Timestamp

trait StoreService {
    def listEnvs(projectId: Int): Seq[Environment]
    def listEnvs(projectId: Int, start : Int, limit : Int): Seq[Environment]
    def listEnvs(projectId: Int, statuses: Seq[EnvStatus]): Seq[Environment]
    def countEnvs(projectId: Int): Int
    def countEnvs(projectId: Int, statuses: Seq[EnvStatus]): Int

    def findEnv(id: Int, projectId: Int): Option[Environment]
    def findEnvWithWorkflow(id: Int, projectId: Int): Option[(Environment, Option[Workflow])]
    def findEnv(id: Int): Option[Environment]
    def findEnv(envName: String, projectId: Int): Option[Environment]

    def isEnvExist(projectId: Int, envId: Int): Boolean

    def getVm(instanceId: String): (Environment, VirtualMachine)

    def listVms(env: Environment): Seq[VirtualMachine]
    def listServers(env: Environment): Seq[BorrowedMachine]

    def listWorkflows(env: Environment): Seq[Workflow]
    def listWorkflows(env: Environment, pageOffset: Int, pageLength: Int): Seq[Workflow]
    def countWorkflows(env: Environment): Int

    def listEnvsWithWorkflow(projectId: Int): Seq[(Environment, Option[Workflow])]
    def listEnvsWithWorkflow(projectId: Int, statuses: Seq[EnvStatus]): Seq[(Environment, Option[Workflow])]
    def listEnvsWithWorkflow(projectId: Int, start : Int, limit : Int): Seq[(Environment, Option[Workflow])]

    def workflowsHistory(env : Environment, pageOffset: Int, pageLength: Int): Seq[(Workflow, Seq[WorkflowStep])]

    def listWorkflowSteps(workflow : Workflow): Seq[WorkflowStep]

    // TODO @throws(classOf[WorkflowRequestFailed])
    def createEnv(env: Environment, workflow: Workflow): Either[Mistake, (Environment, Workflow)]

    def updateEnv(env: Environment)

    def resetEnvStatus(env: Environment): Option[Mistake]

    def updateWorkflow(w: Workflow)

    def createVm(vm: VirtualMachine): VirtualMachine

    def updateServer(vm: EnvResource)

    def createBM(bm: BorrowedMachine): BorrowedMachine

    // TODO @throws(classOf[WorkflowRequestFailed])
    def requestWorkflow(env: Environment, workflow: Workflow): Either[Mistake, (Environment, Workflow)]

    def retrieveWorkflow(envId: Int, projectId: Int): (Environment, Workflow)

    def startWorkflow(envId: Int, projectId: Int): (Environment, Workflow, Seq[EnvResource])

    def finishWorkflow(env: Environment, workflow: Workflow)

    def updateStep(step : WorkflowStep)

    def updateStepStatus(stepId: Int, status: WorkflowStepStatus)

    def updateStepDetailsAndStatus(stepId: Int, details: Option[String], status: WorkflowStepStatus)

    def insertWorkflowSteps(steps : Seq[WorkflowStep])

    def insertWorkflowStep(step : WorkflowStep): WorkflowStep

    def allocateStepCounters(count : Int = 1) : Int
  
    def writeLog(stepId: Int, message: String, timestamp: Timestamp)
    def writeActionLog(actionUUID: String, message: String, timestamp: Timestamp)

    def getLogs(stepId: Int) : Seq[StepLogEntry]

    def getLogs(actionUUID: String) : Seq[StepLogEntry]

    def startAction(actionTracking: ActionTracking): ActionTracking
    def endAction(uuid: String, message: Option[String], status: ActionTrackingStatus.ActionStatus)
    def cancelRunningActions(stepId: Int)
    def getActionLog(stepId: Int) : List[ActionTracking]
    def findBorrowedMachinesByServerId(serverId: Int): Seq[BorrowedMachine]
}

class StoreServiceException extends RuntimeException

class WorkflowRequestFailed(val env: Environment,
                            val workflow: Workflow) extends StoreServiceException
