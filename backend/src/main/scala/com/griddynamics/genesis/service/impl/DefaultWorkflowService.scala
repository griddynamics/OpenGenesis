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
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.service.workflow.{Environment, WorkflowException, WorkflowFuture, WorkflowService}
import com.griddynamics.genesis.model
import model.{WorkflowStatus, Workflow, EnvStatus}
import com.griddynamics.genesis.bean.RequestDispatcher
import com.griddynamics.genesis.service

class DefaultWorkflowService(val storeService: service.StoreService,
                             val dispatcher: RequestDispatcher) extends WorkflowService {

    class DefaultWorkflowFuture(val envName: String) extends WorkflowFuture {
        def suspend() {}

        def resume() {}

        def getStatus = {
            null
        }

        def cancel() {
            dispatcher.cancelWorkflow(envName)
        }
    }

    class DefaultEnvironment(val env: model.Environment) extends Environment {
        def currentWorkflow = new DefaultWorkflowFuture(env.name)

        def listWorkflows = {
            for (wf <- storeService.listWorkflows(env)) yield new DefaultWorkflowFuture(env.name)
        }

        def executeWorkflow(steps: Seq[GenesisStep]) = {
            val workflow = new Workflow(env.id, null,
                WorkflowStatus.Requested, 0, 0, null, None)

            storeService.requestWorkflow(env, workflow) match {
                case Left(m) => throw new WorkflowException(m.toString)
                case Right(_) => {
                    dispatcher.startWorkflow(env.name)
                    new DefaultWorkflowFuture(env.name)
                }
            }
        }

        def destroy(steps: Seq[GenesisStep]) = {
            val workflow = new Workflow(env.id, null,
                WorkflowStatus.Requested, 0, 0, null, None)

            storeService.requestWorkflow(env, workflow) match {
                case Left(m) => throw new WorkflowException(m.toString)
                case Right(_) => {
                    dispatcher.destroyEnv(env.name)
                    new DefaultWorkflowFuture(env.name)
                }
            }
        }
    }

    def listEnvironments = {
        for (env <- storeService.listEnvs()) yield new DefaultEnvironment(env)
    }

    def getEnvironment(envName: String) = {
        storeService.findEnv(envName) match {
            case Some(env) => new DefaultEnvironment(env)
            case _ => throw new WorkflowException("environment '%s' is not found")
        }
    }

    def createEnvironment(projectId: Int, envName: String, envCreator: String, steps: Seq[GenesisStep]) = {
        val env = new model.Environment(envName, EnvStatus.Requested(null),
            envCreator, null, null, projectId)
        val workflow = new Workflow(env.id, null,
            WorkflowStatus.Requested, 0, 0, null, None)

        storeService.createEnv(env, workflow) match {
            case Left(m) => throw new WorkflowException(m.toString)
            case Right(_) => {
                dispatcher.createEnv(env.name)
                new DefaultEnvironment(env)
            }
        }
    }
}