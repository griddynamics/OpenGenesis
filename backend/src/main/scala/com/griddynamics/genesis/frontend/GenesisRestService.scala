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
package com.griddynamics.genesis.frontend

import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service
import com.griddynamics.genesis.bean.RequestBroker
import GenesisRestService._
import com.griddynamics.genesis.model
import model.{EnvStatusField, Workflow, EnvStatus}
import com.griddynamics.genesis.template.TemplateRepository
import service.{ComputeService, TemplateService, StoreService}

class GenesisRestService(storeService: StoreService,
                         templateService: TemplateService,
                         computeService: ComputeService,
                         templateRepository : TemplateRepository,
                         broker: RequestBroker) extends GenesisService {


    def listEnvs(projectId: Int) = {
        for ((env, workflowOption) <- storeService.listEnvsWithWorkflow(projectId)) yield
            Environment(env.name, envStatusDesc(env.status), stepsCompleted(workflowOption),
                env.creator, env.templateName, env.templateVersion, env.projectId)
    }

    def listEnvs(projectId: Int, start : Int, limit : Int) = {
        for ((env, workflowOption) <- storeService.listEnvsWithWorkflow(projectId, start, limit)) yield
            Environment(env.name, envStatusDesc(env.status), stepsCompleted(workflowOption),
                env.creator, env.templateName, env.templateVersion, env.projectId)
    }

    def countEnvs(projectId: Int) : Int = storeService.countEnvs(projectId)

    def listTemplates = {
        for {(name, version) <- templateService.listTemplates.toSeq
             templateOpt = templateService.findTemplate(name, version)
             if templateOpt.nonEmpty
        } yield templateDesc(name, version, templateOpt.get)
    }

    def createEnv(projectId: Int, envName: String, creator: String, templateName: String,
                  templateVersion: String, variables: Map[String, String]) = {
        broker.createEnv(projectId, envName, creator, templateName, templateVersion, variables)
    }

    def destroyEnv(envName: String, variables: Map[String, String]) = {
        broker.destroyEnv(envName, variables)
    }

    def requestWorkflow(envName: String, workflowName: String, variables: Map[String, String]) = {
        broker.requestWorkflow(envName, workflowName, variables)
    }

    def cancelWorkflow(envName: String) {
        broker.cancelWorkflow(envName)
    }


    def isEnvExists(envName: String, projectId: Int): Boolean = {
      storeService.isEnvExist(projectId, envName)
    }

    def describeEnv(envName: String) = {
        storeService.findEnv(envName) match {
            case Some(env) =>
                templateService.findTemplate(env.templateName, env.templateVersion).map(
                    envDesc(
                        env,
                        storeService.listVms(env),
                        storeService.workflowsHistory(env),
                        _,
                        computeService
                    )
                )
            case None => None
        }
    }

    def getLogs(envName: String,  stepId: Int) : Seq[String] =
      storeService.getLogs(stepId).map(log => "%s: %s".format(log.timestamp, log.message))

    def queryVariables(templateName: String, templateVersion: String, workflow: String, variables: Map[String, String]) = {
        templateService.findTemplate(templateName, templateVersion).flatMap {t => {
                t.getWorkflow(workflow).map(workflow => {
                    workflow.partial(variables).map(v => Variable(v.name, v.description, v.isOptional,
                        v.defaultValue, v.values))
                })
            }
        }
    }
}

object GenesisRestService {

  private def stepsCompleted(workflowOption: Option[Workflow]) = {
      workflowOption match {
        case Some(workflow) if workflow.stepsCount > 0 =>
          Some(workflow.stepsFinished / (workflow.stepsCount: Double))
        case Some(workflow) =>
          Some(0.0)
        case None => None
      }
    }

    def templateDesc(name: String, version: String, template: service.TemplateDefinition) = {
        val createWorkflow = workflowDesc(template.createWorkflow)
        Template(name, version, createWorkflow)
    }

    def workflowDesc(workflow: service.WorkflowDefinition) = {
        val vars = for (variable <- workflow.variableDescriptions) yield Variable(variable.name, variable.description, variable.isOptional,
            variable.defaultValue, variable.values)
        Workflow(workflow.name, vars)
    }

    def envDesc(env: model.Environment,
                vms: Seq[model.VirtualMachine],
                history: Seq[(Workflow, Seq[model.WorkflowStep])],
                template: service.TemplateDefinition,
                computeService: ComputeService) = {

        val workflows = for (wf <- template.listWorkflows) yield workflowDesc(wf)

        val vmDescs = for (vm <- vms) yield vmDesc(env, vm, computeService)

        EnvironmentDetails(
            env.name,
            envStatusDesc(env.status),
            env.creator,
            env.templateName,
            env.templateVersion,
            workflows.toSeq,
            template.createWorkflow.name,
            template.destroyWorkflow.name,
            vmDescs.toSeq,
            env.projectId,
            workflowHistoryDesc(history)
        )
    }

    def workflowHistoryDesc(history: Seq[(Workflow, Seq[model.WorkflowStep])]) =
        wrap(history)(() =>
            (for ((flow, steps) <- history) yield
                new WorkflowDetails(flow.name, flow.status.toString, stepsCompleted(Some(flow)), stepDesc(steps), flow.executionStarted.map (_.getTime))).toSeq)

    def stepDesc(steps : Seq[model.WorkflowStep]) =
        wrap(steps)(() =>
            (for (step <- steps) yield
                new WorkflowStep(step.id.toString, step.phase, step.status.toString, step.details)).toSeq)

    def wrap[A](seq : Traversable[_])(f : () => A): Option[A] = {
        if(seq.isEmpty) None
        else Some(f())
    }

    def envStatusDesc(status: EnvStatus) = {
        status match {
            case EnvStatus.Ready() => "Ready"
            case EnvStatus.Destroyed() => "Destroyed"
            case s => s.toString
        }
    }

    def vmDesc(env: model.Environment, vm: model.VirtualMachine, computeService: ComputeService) = {
      val status:EnvStatus = EnvStatusField.envStatusFieldToStatus(env.status);
      val ipAddressOtp =
        status match {
          case EnvStatus.Destroyed() => None
          case _ => computeService.getIpAddresses(vm)
        }
        val ipAddress = ipAddressOtp.getOrElse(model.IpAddresses())

        VirtualMachine(
            envName = env.name,
            roleName = vm.roleName,
            hostNumber = vm.id,
            instanceId = vm.instanceId.getOrElse("unknown"),
            hardwareId = vm.hardwareId.getOrElse("unknown"),
            imageId = vm.imageId.getOrElse("unknown"),
            publicIp = ipAddress.publicIp.getOrElse("unknown"),
            privateIp = ipAddress.privateIp.getOrElse("unknown"),
            status = vm.status.toString
        )
    }
}
