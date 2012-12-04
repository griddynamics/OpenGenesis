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

import GenesisRestService._
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.{model, service}
import com.griddynamics.genesis.model.{Workflow, EnvStatus}
import com.griddynamics.genesis.repository.ConfigurationRepository
import com.griddynamics.genesis.service._
import com.griddynamics.genesis.validation.Validation._

class GenesisRestService(storeService: StoreService,
                         templateService: TemplateService,
                         computeService: ComputeService,
                         broker: RequestBroker,
                         envAccessService: EnvironmentAccessService,
                         configurationRepository: ConfigurationRepository) extends GenesisService {


    def listEnvs(projectId: Int, statusFilter: Option[Seq[String]] = None, ordering: Option[Ordering] = None) = {
      val filterOpt = statusFilter.map(_.map(EnvStatus.withName(_)))
      envs (
        storeService.listEnvsWithWorkflow(projectId, filterOpt, ordering),
        configurationRepository.lookupNames(projectId)
      )
    }

    def countEnvs(projectId: Int) : Int = storeService.countEnvs(projectId)

    def listTemplates(projectId: Int) =
        for {(name, version) <- templateService.listTemplates(projectId)} yield Template(name, version, null, Seq())

    def createEnv(projectId: Int, envName: String, creator: String, templateName: String,
                  templateVersion: String, variables: Map[String, String], config: Configuration) = {
        val result = broker.createEnv(projectId, envName, creator, templateName, templateVersion, variables, config)

        result match {
          case Success(envId, true) => {
            config.id.foreach { cId =>
              val (users, groups) = envAccessService.getConfigAccessGrantees(cId)
              envAccessService.grantAccess(envId, users, groups)
            }
            Success(envId)
          }
          case other => other
        }
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
                        storeService.countFinishedActionsForCurrentWorkflow(env),
                        stepsCompleted(flow),
                        configurationRepository.get(projectId, env.configurationId)
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

    def getLogs(envId: Int,  stepId: Int, includeActions: Boolean) : Seq[StepLogEntry] =
      storeService.getLogs(stepId, includeActions).map { entry => StepLogEntry(entry.timestamp, entry.message) }

    def getLogs(envId: Int, actionUUID: String): Seq[StepLogEntry] =
      storeService.getLogs(actionUUID).map { entry => StepLogEntry(entry.timestamp, entry.message) }

    def queryVariables(projectId: Int, templateName: String, templateVersion: String, workflow: String, variables: Map[String, String]) = {
        templateService.findTemplate(projectId, templateName, templateVersion).flatMap {t => {
                t.getWorkflow(workflow).map(workflow => {
                    workflow.partial(variables).map(varDesc(_))
                })
            }
        }
    }

    def getTemplate(projectId: Int, templateName: String, templateVersion: String) =
    templateService.findTemplate(projectId, templateName, templateVersion).map(templateDesc(templateName, templateVersion, _))

    def getStepLog(stepId: Int) = storeService.getActionLog(stepId).map { action =>
      new ActionTracking(action.actionUUID, action.actionName, action.description, action.started.getTime, action.finished.map(_.getTime), action.status.toString)
    }

    def getWorkflow (projectId: Int, templateName: String, templateVersion: String, workflowName: String) : ExtendedResult[com.griddynamics.genesis.api.Workflow] =  {
        templateService.findTemplate(projectId, templateName, templateVersion).map(_.getValidWorkflow(workflowName)) match {
            case Some(x) => x.map(workflowDesc(_))
            case _ => Failure(isNotFound = true)
        }
    }

    def updateEnvironmentName(envId: Int, projectId: Int, newName: String) = {
       validateNewName(envId, projectId, newName).map(name => storeService.updateEnvName(envId, name))
    }

    private def validateNewName(envId: Int, projectId: Int, name: String) : ExtendedResult[String] = {
        must(name, "Environment [%d] not found".format(envId)) {
            e => storeService.findEnv(envId).isDefined
        } ++
        must(name, "Environment with name '" + name + "' already exists") {
              name => storeService.findEnv(name, projectId).map(e => e.id == envId).getOrElse(true)
        } ++
        mustMatchProjectEnvName(name, name, "Name")
    }

  def stepExists(stepId: Int, envId: Int) = storeService.stepExists(stepId, envId)

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
        val createWorkflow = Workflow(template.createWorkflow.name, Seq())
        val workflows = for (wf <- template.listWorkflows) yield Workflow(wf.name, Seq())
        Template(name, version, createWorkflow, workflows)
    }

    def workflowDesc(workflow: service.WorkflowDefinition) = {
        val vars = for (variable <- workflow.variableDescriptions) yield varDesc(variable)
        Workflow(workflow.name, vars)
    }

    def envDesc(env: model.Environment,
                vms: Seq[model.VirtualMachine],
                bms: Seq[model.BorrowedMachine],
                template: service.TemplateDescription,
                computeService: ComputeService,
                historyCount: Int,
                currentWorkflowFinishedActionsCount: Int,
                workflowCompleted: Option[Double],
                config: Option[Configuration]) = {

        val workflows = template.workflows.map (Workflow(_, Seq()))

        val vmDescs = for (vm <- vms) yield vmDesc(env, vm, computeService)

        val bmDescs = for (server <- bms) yield serversDesc(env, server)

        EnvironmentDetails(
            env.id,
            env.name,
            env.status.toString,
            env.creator,
            env.creationTime.getTime,
            env.modificationTime.map(_.getTime),
            env.modifiedBy,
            env.templateName,
            env.templateVersion,
            workflows,
            template.createWorkflow,
            template.destroyWorkflow,
            vmDescs.toSeq,
            bmDescs.toSeq,
            env.projectId,
            historyCount,
            currentWorkflowFinishedActionsCount,
            workflowCompleted,
            attrDesc(env.deploymentAttrs),
            config.map(_.name).getOrElse("*deleted*")
        )
    }

    def workflowHistoryDesc(history: Seq[(Workflow, Seq[model.WorkflowStep])], workflowsTotalCount: Int) = {
        val h = wrap(history)(() =>
            (for ((flow, steps) <- history) yield
                new WorkflowDetails(flow.id, flow.name, flow.status.toString, flow.startedBy, flow.displayVariables, stepsCompleted(Some(flow)),
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
                  step.finished.map(_.getTime),
                  step.title,
                  step.regular
            )).toSeq)

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

  private def attrDesc(attrs: Seq[model.DeploymentAttribute]) = attrs.map( attr => attr.key -> Attribute(attr.value, attr.desc)).toMap

  private def envs(envs: Seq[(model.Environment, Option[Workflow])], configNames: Map[Int, String]) = for ((env, workflowOption) <- envs) yield
    Environment(env.id, env.name, env.status.toString, stepsCompleted(workflowOption), env.creator,
      env.creationTime.getTime, env.modificationTime.map(_.getTime), env.modifiedBy,
      env.templateName, env.templateVersion, env.projectId,
      attrDesc(env.deploymentAttrs), configNames.getOrElse(env.configurationId, "*deleted*"))

  private def varDesc(v : VariableDescription) = Variable(v.name, v.clazz.getSimpleName, v.description, v.isOptional,
    v.defaultValue, v.values.toMap, v.dependsOn, v.group)
}
