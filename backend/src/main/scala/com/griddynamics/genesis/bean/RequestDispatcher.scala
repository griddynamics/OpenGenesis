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
package com.griddynamics.genesis.bean

import scala.collection.mutable
import java.util.concurrent.ExecutorService
import akka.actor.TypedActor
import com.griddynamics.genesis.workflow.actor.{TypedFlowCoordinatorImpl, TypedFlowCoordinator}
import com.griddynamics.genesis.plugin.{Cancel, StepCoordinatorFactory, GenesisStep}
import com.griddynamics.genesis.core._
import com.griddynamics.genesis.service.{TemplateService, StoreService}
import collection.mutable.ArrayBuffer

trait RequestDispatcher {
    def createEnv(envName: String)

    def startWorkflow(envName: String)

    def destroyEnv(envName: String)

    def cancelWorkflow(envName: String)
}

class RequestDispatcherImpl(beatPeriodMs: Long,
                            flowTimeOutMs: Long,
                            storeService: StoreService,
                            templateService: TemplateService,
                            executorService: ExecutorService,
                            stepCoordinatorFactory: StepCoordinatorFactory)
    extends TypedActor with RequestDispatcher {

    val coordinators = mutable.Map[String, TypedFlowCoordinator]()

    def createEnv(envName: String) {
        startWorkflow(envName)
    }

    def destroyEnv(envName: String) {
        startWorkflow(envName)
    }

    def startWorkflow(envName: String) {
        val (env, workflow) = storeService.retrieveWorkflow(envName)

        val definition = templateService.findTemplate(env.projectId, env.templateName, env.templateVersion)
        val rawSteps = definition.flatMap(_.getWorkflow(workflow.name)
            .map(_.embody(workflow.variables, Option(env.name))))
        rawSteps.map(sortByPhase).map(applyIds(_)).foreach(s => {
            coordinators(env.name) = if (Option(workflow.name) == definition.map(_.destroyWorkflow.name))
                destroyingCoordinator(envName, s)
            else regularCoordinator(envName, s)

            coordinators(env.name).start
        })
    }

    def sortByPhase(rawSteps: Seq[GenesisStep]): Seq[GenesisStep] = {
      val sorted: mutable.ArrayBuffer[GenesisStep]  = new ArrayBuffer[GenesisStep]()
      var toBeProcessed = rawSteps;

      while(sorted.size != rawSteps.size) {
        val (noPrecedingSteps, havePrecedingSteps) = toBeProcessed.partition (
          step => step.precedingPhases.forall(phase=> toBeProcessed.find(_.phase == phase).isEmpty)
        )
        if(noPrecedingSteps.isEmpty && !havePrecedingSteps.isEmpty) { // cyclic dependencies, let flow coordinator deal with it
          return rawSteps
        }
        toBeProcessed = havePrecedingSteps
        sorted ++= noPrecedingSteps
      }
      sorted.toSeq
    }

    def cancelWorkflow(envName: String) {
        for (coordinator <- coordinators.get(envName))
            coordinator.signal(Cancel())

        // TODO collect all coordinator's garbage
        coordinators -= envName
    }

    def applyIds(steps: Seq[GenesisStep]) = {
        var counter = storeService.allocateStepCounters(steps.size)
        for (step <- steps) yield {
            counter +=1
            step.copy(id = counter)
        }
    }

    def regularCoordinator(envName: String, flowSteps: Seq[GenesisStep]) =
        new TypedFlowCoordinatorImpl(
            new GenesisFlowCoordinator(envName, flowSteps, storeService,
                stepCoordinatorFactory) with RegularWorkflow,
            beatPeriodMs, flowTimeOutMs, executorService
        )

    def destroyingCoordinator(envName: String, flowSteps: Seq[GenesisStep]) =
        new TypedFlowCoordinatorImpl(
            new GenesisFlowCoordinator(envName, flowSteps, storeService,
                stepCoordinatorFactory) with DestroyWorkflow,
            beatPeriodMs, flowTimeOutMs, executorService
        )
}