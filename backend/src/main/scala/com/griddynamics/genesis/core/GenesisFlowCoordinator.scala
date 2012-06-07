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
package com.griddynamics.genesis.core

import scala.collection.mutable
import com.griddynamics.genesis.plugin._
import com.griddynamics.genesis.workflow.{StepCoordinator, Signal, StepResult, FlowCoordinator}
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.workflow.signal.{Success, Fail}
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.common.Mistake
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.step.CoordinatorThrowable
import com.griddynamics.genesis.logging.LoggerWrapper

abstract class GenesisFlowCoordinator(envName: String,
                             flowSteps: Seq[GenesisStep],
                             storeService: StoreService,
                             stepCoordinatorFactory: StepCoordinatorFactory)
    extends GenesisFlowCoordinatorBase(envName, flowSteps, storeService, stepCoordinatorFactory)
    with StepIgnore with StepRestart with StepExecutionContextHolder

abstract class GenesisFlowCoordinatorBase(val envName: String,
                                          val flowSteps: Seq[GenesisStep],
                                          val storeService: StoreService,
                                          val stepCoordinatorFactory: StepCoordinatorFactory)
    extends FlowCoordinator with Logging {

    var env: Environment = _
    var vms: Seq[VirtualMachine] = _

    var workflow: Workflow = _

    var stepsToStart = flowSteps
    var finishedSteps = Seq[GenesisStep]()

    val onFlowFinishSuccess : EnvStatus

    def flowDescription = {
      val workflowName = if (workflow == null) "*undefined at this stage*" else workflow.name
      "Workflow[env='%s', name='%s']".format(envName, workflowName)
    }

    def onFlowStart() = {
        val (iEnv, iWorkflow, iVms) = storeService.startWorkflow(envName)

        env = iEnv
        vms = iVms
        workflow = iWorkflow

        workflow.stepsCount = stepsToStart.size
        storeService.updateWorkflow(workflow)

        stepsToStart = persistSteps(stepsToStart) //persist steps & generate step ids

        Right(createReachableStepCoordinators())
    }

    def onFlowFinish(signal: Signal) {
        import WorkflowStatus._
        val (workflowStatus, envStatus) = signal match {
            case Success() => (Succeed, onFlowFinishSuccess)
            case Cancel() => (Canceled, EnvStatus.Canceled(workflow.name))
            case _ => (Failed, EnvStatus.Failed(workflow.name))
        }
        workflow.status = workflowStatus
        env.status = envStatus
        storeService.finishWorkflow(env, workflow)
    }

    def onStepFinish(result: StepResult) = result match {
        case coordinatorThrowable: CoordinatorThrowable => Left(Fail(Mistake(coordinatorThrowable.throwable)))
        case genesisStep  => onStepFinish(genesisStep.asInstanceOf[GenesisStepResult]) //todo ClassCastException is still possible
    }

    //TODO remove workflow.stepsFinished
    def onStepFinish(result: GenesisStepResult): Either[Signal, Seq[StepCoordinator]] = {
        workflow.stepsFinished += 1
        storeService.updateWorkflow(workflow)

        finishedSteps = finishedSteps :+ result.step

        Right(createReachableStepCoordinators())
    }

    def createReachableStepCoordinators() = {
        for (step <- detectReachableSteps())
        yield createStepCoordinator(step)
    }

    def createStepCoordinator(step: GenesisStep) = {
        val context = createStepExecutionContext(step)
        val stepCoordinator = stepCoordinatorFactory.apply(step.actualStep, context)
        new GenesisStepCoordinator(step, workflow, stepCoordinator, storeService)
    }

    def detectReachableSteps() = {
        val finishedStepsIds = finishedSteps.map(_.id)
        val unfinishedPhases = flowSteps.filter(s => !finishedStepsIds.contains(s.id))
                                        .map(_.phase).toSet

        val (reachableSteps, unreachableSteps) = stepsToStart.partition {
            step => step.precedingPhases.forall {
                phase => !unfinishedPhases.contains(phase)
            }
        }

        stepsToStart = unreachableSteps
        reachableSteps
    }

    def createStepExecutionContext(step: GenesisStep): StepExecutionContext

    private def persistSteps(steps : Seq[GenesisStep]) =
        for (step <- steps) yield {
            val workflowStep =
                storeService.insertWorkflowStep(
                    WorkflowStep(
                        step.id,
                        workflow.id,
                        step.phase,
                        WorkflowStepStatus.Requested,
                        step.actualStep.stepDescription
                    )
                )
            step.copy(id = workflowStep.id)
        }

}

trait StepIgnore extends GenesisFlowCoordinatorBase {
    abstract override def onStepFinish(result: GenesisStepResult) = {
        if (result.isStepFailed && !result.step.ignoreFail)
            Left(Fail(Mistake("Step '%s' failed".format(result.step))))
        else
            super.onStepFinish(result)
    }
}

trait StepRestart extends GenesisFlowCoordinatorBase {
    val retryMap = mutable.Map[GenesisEntity.Id, Int]() // step.id -> retryCount

    abstract override def detectReachableSteps() = {
        val steps = super.detectReachableSteps()

        for (step <- steps)
            retryMap(step.id) = step.retryCount

        steps
    }

    abstract override def onStepFinish(result: GenesisStepResult) = {
        val retriesRemain = retryMap(result.step.id)

        if (result.isStepFailed && retriesRemain > 0) {
            retryMap(result.step.id) = retriesRemain - 1
            Right(Seq(createStepCoordinator(result.step)))
        } else
            super.onStepFinish(result)
    }
}

trait StepExecutionContextHolder extends GenesisFlowCoordinatorBase {
    val flowStepsTemplate = new FlowStepsTemplate(flowSteps)

    var envState: Option[GenesisEntity.Id] = None
    val vmsState = mutable.Map[GenesisEntity.Id, GenesisEntity.Id]() // vm.id -> step.id

    val globals = mutable.Map[String, AnyRef]()
    val pluginContext = mutable.Map[String,Any]()

    def createStepExecutionContext(step: GenesisStep) =
        new StepExecutionContextImpl(step, env.copy(), vms.map(_.copy()), workflow.copy(), globals, pluginContext)

    abstract override def onStepFinish(result: GenesisStepResult) = {
        handleEnvState(result)
        handleVmsState(result)
        if(!result.isStepFailed) {
            exportToContext(result)
        }
        super.onStepFinish(result)
    }

    private def exportToContext(result: GenesisStepResult) {
      result.step.exportTo.foreach { case (from, to) =>
        try {
          val actualResult: StepResult = result.actualResult.getOrElse(null)
          globals(to) = actualResult.getClass.getDeclaredMethod(from).invoke(actualResult)
        } catch {
          case e => {
            LoggerWrapper.writeLog(result.step.id, "Failed to export step result to context: export settings = " + result.step.exportTo)
            throw e
          }
        }
      }
    }

    def handleEnvState(result: GenesisStepResult) {
        for (uEnv <- result.envUpdate) {
            if (envState.isDefined && flowStepsTemplate.isStepsParallel(envState.get, result.step.id))
                log.warn("Parallel modification of env '%s' detected on step '%s'", uEnv, result.step)

            env = uEnv
            envState = Some(result.step.id)
        }
    }

    def handleVmsState(result: GenesisStepResult) {
        for (uVm <- result.vmsUpdate) {
            val vmsStateId = vmsState.get(uVm.id)

            if (vmsStateId.isDefined && flowStepsTemplate.isStepsParallel(vmsStateId.get, result.step.id))
                log.warn("Parallel modification of vm '%s' detected on step '%s'", uVm, result.step)

            updateVm(uVm)
            vmsState(uVm.id) = result.step.id
        }
    }

    def updateVm(vm: VirtualMachine) {
        val index = vms.indexWhere(_.id == vm.id)

        if (index == -1)
            vms = vms :+ vm
        else
            vms = vms.updated(index, vm)
    }
}

trait RegularWorkflow { this: GenesisFlowCoordinatorBase =>
    override val onFlowFinishSuccess = EnvStatus.Ready()
}

trait DestroyWorkflow { this: GenesisFlowCoordinatorBase =>
    override val onFlowFinishSuccess = EnvStatus.Destroyed()
}
