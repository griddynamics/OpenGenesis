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
package com.griddynamics.genesis.bean

import scala.collection.mutable
import java.util.concurrent.ExecutorService
import akka.actor.ActorSystem
import com.griddynamics.genesis.workflow.actor.{TypedFlowCoordinatorImpl, TypedFlowCoordinator}
import com.griddynamics.genesis.core._
import com.griddynamics.genesis.service.{RemoteAgentsService, Builders, TemplateService, StoreService}
import collection.mutable.ArrayBuffer
import com.griddynamics.genesis.plugin.{StepBuilder, Cancel, StepCoordinatorFactory}
import com.griddynamics.genesis.model.EnvStatus
import com.griddynamics.genesis.model.WorkflowStatus._
import com.griddynamics.genesis.util.Logging
import java.sql.Timestamp
import com.griddynamics.genesis.configuration.WorkflowConfig

trait RequestDispatcher {
    def createEnv(envName: Int, projectId: Int)

    def startWorkflow(envId: Int, projectId: Int)

    def destroyEnv(envId: Int, projectId: Int)

    def cancelWorkflow(envId: Int, projectId: Int)
}

class RequestDispatcherImpl(workflowConfig: WorkflowConfig,
                            storeService: StoreService,
                            templateService: TemplateService,
                            executorService: ExecutorService,
                            stepCoordinatorFactory: StepCoordinatorFactory,
                            actorSystem: ActorSystem,
                            remoteAgentService: RemoteAgentsService) extends RequestDispatcher with Logging {

    val coordinators = mutable.Map[(Int, Int), TypedFlowCoordinator]()

    def createEnv(envId: Int, projectId: Int) {
        startWorkflow(envId, projectId)
    }

    def destroyEnv(envId: Int, projectId: Int) {
        startWorkflow(envId, projectId)
    }

    def startWorkflow(envId: Int, projectId: Int) {
      val (env, workflow) = storeService.retrieveWorkflow(envId, projectId)

      try{
        val definition = templateService.findTemplate(env)
        val rawSteps = definition.flatMap(_.getWorkflow(workflow.name)
            .map(_.embody(workflow.variables, Option(env.id), Option(env.projectId)))).getOrElse(Builders(Seq()))

        Some(applyIds(sortByPhase(rawSteps.regular).filter(p => !p.skip))).foreach(s => {
            val rescueBuilders: Seq[StepBuilder] = applyIds(sortByPhase(rawSteps.onError.map(f => {
                f.regular = false
                f}
            )).filter(s => !s.skip))
            coordinators((env.id, env.projectId)) = if (Option(workflow.name) == definition.map(_.destroyWorkflow.name))
                destroyingCoordinator(env.id, projectId, s, rescueBuilders)
            else regularCoordinator(env.id, projectId, s, rescueBuilders)
            coordinators((env.id, env.projectId)).start()
        })
      } catch {
        case e: Throwable => {
          log.error(e, "Failed to start workflow [%s] for env [%d]".format(workflow.name, envId))
          env.status = EnvStatus.Broken
          workflow.status = Failed
          workflow.executionStarted = Some(new Timestamp(System.currentTimeMillis()))
          storeService.finishWorkflow(env, workflow)
        }
      }
    }

    def sortByPhase(rawSteps: Seq[StepBuilder]): Seq[StepBuilder] = {
      import scala.collection.JavaConversions._
      val sorted: mutable.ArrayBuffer[StepBuilder]  = new ArrayBuffer[StepBuilder]()
      var toBeProcessed = rawSteps

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

    def cancelWorkflow(envId: Int, projectId: Int) {
        for (coordinator <- coordinators.get((envId, projectId)))
            coordinator.signal(Cancel())

        // TODO collect all coordinator's garbage
        coordinators -= Tuple2(envId, projectId)
    }

    def applyIds(builders: Seq[StepBuilder]): Seq[StepBuilder] = {
        var counter = storeService.allocateStepCounters(builders.size)
        for (builder <- builders) yield {
            counter +=1
            builder.id = counter
            builder
        }
    }

    def regularCoordinator(envId: Int, projectId: Int, flowSteps: Seq[StepBuilder], rescueSteps: Seq[StepBuilder]) =
        new TypedFlowCoordinatorImpl(
            new GenesisFlowCoordinator(envId, projectId, flowSteps, storeService,
                stepCoordinatorFactory, rescueSteps) with RegularWorkflow,
            workflowConfig, executorService, actorSystem, remoteAgentService

        )

    def destroyingCoordinator(envId: Int, projectId: Int, flowSteps: Seq[StepBuilder], rescueSteps: Seq[StepBuilder]) =
        new TypedFlowCoordinatorImpl(
            new GenesisFlowCoordinator(envId, projectId, flowSteps, storeService,
                stepCoordinatorFactory, rescueSteps) with DestroyWorkflow,
            workflowConfig, executorService, actorSystem, remoteAgentService

        )
}