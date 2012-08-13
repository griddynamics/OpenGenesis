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
package com.griddynamics.genesis.frontend

import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service
import com.griddynamics.genesis.bean.RequestBroker
import GenesisRestService._
import com.griddynamics.genesis.model
import model.{Workflow, EnvStatus}
import com.griddynamics.genesis.template.TemplateRepository
import service.{ComputeService, TemplateService, StoreService}

class GenesisRestService(storeService: StoreService,
                         templateService: TemplateService,
                         computeService: ComputeService,
                         templateRepository : TemplateRepository,
                         broker: RequestBroker) extends GenesisService {


    def listEnvs(projectId: Int) = {
        for ((env, workflowOption) <- storeService.listEnvsWithWorkflow(projectId)) yield
            Environment(env.id, env.name, env.status.toString, stepsCompleted(workflowOption),
                env.creator, env.templateName, env.templateVersion, env.projectId)
    }

  def listEnvs(projectId: Int, statuses: Seq[String]) = {
    for ((env, workflowOption) <- storeService.listEnvsWithWorkflow(projectId, statuses.map(EnvStatus.withName(_)))) yield
      Environment(env.id, env.name, env.status.toString, stepsCompleted(workflowOption),
        env.creator, env.templateName, env.templateVersion, env.projectId)
  }

  def listEnvs(projectId: Int, start : Int, limit : Int) = {
        for ((env, workflowOption) <- storeService.listEnvsWithWorkflow(projectId, start, limit)) yield
            Environment(env.id, env.name, env.status.toString, stepsCompleted(workflowOption),
                env.creator, env.templateName, env.templateVersion, env.projectId)
    }

    def countEnvs(projectId: Int) : Int = storeService.countEnvs(projectId)

    def listTemplates(projectId: Int) =
        for {(name, version) <- templateService.listTemplates(projectId)} yield Template(name, version, null, Seq())

    def createEnv(projectId: Int, envName: String, creator: String, templateName: String,
                  templateVersion: String, variables: Map[String, String]) = {
        broker.createEnv(projectId, envName, creator, templateName, templateVersion, variables)
    }

    def destroyEnv(envId: Int, projectId: Int, variables: Map[String, String], startedBy: String) = {
        broker.destroyEnv(envId, projectId, variables, startedBy)
    }

    def requestWorkflow(envId: Int, projectId: Int, workflowName: String, variables: Map[String, String], startedBy: String) = {
        broker.requestWorkflow(envId, projectId, workflowName, variables, startedBy)
    }

    def cancelWorkflow(envId: Int, projectId: Int) {
        broker.cancelWorkflow(envId, projectId)
    }

    def resetEnvStatus(envId: Int, projectId: Int) = {
        broker.resetEnvStatus(envId, projectId)
    }

    def isEnvExists(envId: Int, projectId: Int): Boolean = {
      storeService.isEnvExist(projectId, envId)
    }

    def describeEnv(envId: Int, projectId: Int) = {
        storeService.findEnvWithWorkflow(envId, projectId) match {
            case Some((env, flow)) =>
                templateService.descTemplate(env.projectId, env.templateName, env.templateVersion).map {
                    envDesc(
                        env,
                        storeService.listVms(env),
                        storeService.listServers(env),
                        _,
                        computeService,
                        storeService.countWorkflows(env),
                        stepsCompleted(flow)
                    )
                }
            case None => None
        }
    }

    def workflowHistory(envId: Int, projectId: Int, pageOffset: Int, pageLength: Int): Option[WorkflowHistory] = {
        storeService.findEnv(envId, projectId).map(env =>
            workflowHistoryDesc(
                storeService.workflowsHistory(env, pageOffset, pageLength),
                storeService.countWorkflows(env)
            )
        )
    }

    def getLogs(envId: Int,  stepId: Int) : Seq[String] =
      storeService.getLogs(stepId).map(log => "%s: %s".format(log.timestamp, log.message))

    def getLogs(envId: Int, actionUUID: String) =
      storeService.getLogs(actionUUID).map(log => "%s: %s".format(log.timestamp, log.message))

    def queryVariables(projectId: Int, templateName: String, templateVersion: String, workflow: String, variables: Map[String, String]) = {
        templateService.findTemplate(projectId, templateName, templateVersion).flatMap {t => {
                t.getWorkflow(workflow).map(workflow => {
                    workflow.partial(variables).map(v => Variable(v.name, v.description, v.isOptional,
                        v.defaultValue, v.values, v.dependsOn))
                })
            }
        }
    }

    def getTemplate(projectId: Int, templateName: String, templateVersion: String) =
    templateService.findTemplate(projectId, templateName, templateVersion).map(templateDesc(templateName, templateVersion, _))

    def getStepLog(stepId: Int) = storeService.getActionLog(stepId).map { action =>
      new ActionTracking(action.actionUUID, action.actionName, action.description, action.started.getTime, action.finished.map(_.getTime), action.status.toString)
    }
}

object GenesisRestService {

  private def stepsCompleted(workflowOption: Option[Workflow]) = {
      workflowOption match {
        case Some(workflow) if (workflow.stepsCount > 0 && workflow.stepsFinished > 0) =>
          Some(BigDecimal(workflow.stepsFinished / (workflow.stepsCount: Double)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
        case Some(workflow) if workflow.stepsCount > 0 =>
          Some(0.04)
        case Some(workflow) =>
          Some(0.0)
        case None => None
      }
    }

    def templateDesc(name: String, version: String, template: service.TemplateDefinition) = {
        val createWorkflow = workflowDesc(template.createWorkflow)
        val workflows = for (wf <- template.listWorkflows) yield workflowDesc(wf)
        Template(name, version, createWorkflow, workflows)
    }

    def workflowDesc(workflow: service.WorkflowDefinition) = {
        val vars = for (variable <- workflow.variableDescriptions) yield Variable(variable.name, variable.description, variable.isOptional,
            variable.defaultValue, variable.values.toMap, variable.dependsOn)
        Workflow(workflow.name, vars)
    }

    def envDesc(env: model.Environment,
                vms: Seq[model.VirtualMachine],
                bms: Seq[model.BorrowedMachine],
                template: service.TemplateDescription,
                computeService: ComputeService,
                historyCount: Int,
                workflowCompleted: Option[Double]) = {

        val workflows = template.workflows.map (Workflow(_, Seq()))

        val vmDescs = for (vm <- vms) yield vmDesc(env, vm, computeService)

        val bmDescs = for (server <- bms) yield serversDesc(env, server)

        EnvironmentDetails(
            env.id,
            env.name,
            env.status.toString,
            env.creator,
            env.templateName,
            env.templateVersion,
            workflows,
            template.createWorkflow,
            template.destroyWorkflow,
            vmDescs.toSeq,
            bmDescs.toSeq,
            env.projectId,
            historyCount,
            workflowCompleted,
            env.deploymentAttrs.map( attr => attr.key -> Attribute(attr.value, attr.desc)).toMap
        )
    }

    def workflowHistoryDesc(history: Seq[(Workflow, Seq[model.WorkflowStep])], workflowsTotalCount: Int) = {
        val h = wrap(history)(() =>
            (for ((flow, steps) <- history) yield
                new WorkflowDetails(flow.name, flow.status.toString, flow.startedBy, stepsCompleted(Some(flow)),
                  stepDesc(steps), flow.executionStarted.map (_.getTime), flow.executionFinished.map (_.getTime))).toSeq)

        WorkflowHistory(h, workflowsTotalCount)
    }

    def stepDesc(steps : Seq[model.WorkflowStep]) =
        wrap(steps)(() =>
            (for (step <- steps) yield
                new WorkflowStep(step.id.toString,
                  step.phase,
                  step.status.toString,
                  step.details,
                  step.started.map(_.getTime),
                  step.finished.map(_.getTime))
            ).toSeq)

    def wrap[A](seq : Traversable[_])(f : () => A): Option[A] = {
        if(seq.isEmpty) None
        else Some(f())
    }

    def serversDesc(env: model.Environment, bm: model.BorrowedMachine) = {
      BorrowedMachine(env.name, bm.roleName, bm.instanceId.getOrElse("unknown"), bm.getIp.map(_.address).getOrElse("unknown"), bm.status.toString )
    }

    def vmDesc(env: model.Environment, vm: model.VirtualMachine, computeService: ComputeService) = {
      val ipAddressOtp =
        env.status match {
          case EnvStatus.Destroyed => None
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
