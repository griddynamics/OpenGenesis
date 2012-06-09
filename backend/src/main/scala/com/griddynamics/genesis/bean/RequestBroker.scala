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

import com.griddynamics.genesis.model.{Workflow, Environment}
import com.griddynamics.genesis.model.EnvStatus
import com.griddynamics.genesis.model.WorkflowStatus
import com.griddynamics.genesis.api.{RequestResult => RR}
import com.griddynamics.genesis.service
import service.{ValidationError, TemplateService, StoreService}

trait RequestBroker {
    def createEnv(projectId: Int, envName: String, envCreator : String,
                  templateName: String, templateVersion: String,
                  variables: Map[String, String]) : RR

    def requestWorkflow(envName: String, workflowName: String, variables: Map[String, String]) : RR

    def destroyEnv(envName: String, variables: Map[String, String]) : RR

    def cancelWorkflow(envName : String)
}

class RequestBrokerImpl(storeService: StoreService,
                        templateService: TemplateService,
                        dispatcher: RequestDispatcher) extends RequestBroker {
    import RequestBrokerImpl._

    def createEnv(projectId: Int, envName: String, envCreator : String,
                  templateName: String, templateVersion: String,
                  variables: Map[String, String]) : RR = {
        validateEnvName(envName) match {
            case Some(rr) => return rr
            case None =>
        }

        validateCreator(envCreator) match {
            case Some(rr) => return rr
            case _ =>
        }

        val twf = templateService.findTemplate(projectId, templateName, templateVersion) match  {
            case None => return RR(compoundVariablesErrors = Seq("Template %s with version %s not found".format(templateName, templateVersion)),
                isSuccess = false, isNotFound = true)
            case Some(template) => template.createWorkflow
        }

        validateWorkflow(twf, variables, envName) match {
            case Some(rr) => return rr
            case None =>
        }

        val env = new Environment(envName, EnvStatus.Requested(twf.name),
                                  envCreator, templateName, templateVersion, projectId)
        val workflow = new Workflow(env.id, twf.name,
                                    WorkflowStatus.Requested, 0, 0, variables, None)

        storeService.createEnv(env, workflow) match {
            case Left(m) => RR(compoundVariablesErrors = Seq(m.toString))
            case Right(_) => {
                dispatcher.createEnv(env.name)
                RR(isSuccess = true)
            }
        }
    }

    def destroyEnv(envName: String, variables: Map[String, String]) : RR = {
        val env = findEnv(envName) match {
            case Right(e) => e
            case Left(rr) => return rr
        }

        val twf = templateService.findTemplate(env.projectId, env.templateName, env.templateVersion).get.destroyWorkflow

        validateWorkflow(twf, variables, envName) match {
            case Some(rr) => return rr
            case None =>
        }

        val workflow = new Workflow(env.id, twf.name,
                                    WorkflowStatus.Requested, 0, 0, variables, None)

        storeService.requestWorkflow(env, workflow) match {
            case Left(m) => RR(compoundVariablesErrors =  Seq(m.toString))
            case Right(_) => {
                dispatcher.destroyEnv(env.name)
                RR(isSuccess = true)
            }
        }
    }

    def requestWorkflow(envName: String, workflowName: String,
                        variables: Map[String, String]) : RR = {
        val env = findEnv(envName) match {
            case Right(e) => e
            case Left(rr) => return rr
        }

        val template = templateService.findTemplate(env.projectId, env.templateName, env.templateVersion)
        if (template.filter(workflowName == _.createWorkflow.name).isDefined) {
            return RR(serviceErrors = Map(RR.envName ->
                "It's not allowed to execute create workflow['%s'] in existing environment '%s'"
                    .format(workflowName, envName)))
        }
        val twf = template.get.getWorkflow(workflowName)

        if (twf.isEmpty) {
            return RR(serviceErrors = Map(RR.envName ->
                "Failed to find workflow with name '%s' in environment '%s'".format(workflowName, envName)))
        }
        validateWorkflow(twf.get, variables, envName) match {
            case Some(rr) => return rr
            case None =>
        }

        val workflow = new Workflow(env.id, workflowName, WorkflowStatus.Requested, 0, 0, variables, None)

        storeService.requestWorkflow(env, workflow)  match {
            case Left(m) => RR(compoundVariablesErrors = Seq(m.toString))
            case Right(_) => {
                dispatcher.startWorkflow(env.name)
                RR(isSuccess = true)
            }
        }
    }

    def cancelWorkflow(envName : String) {
        dispatcher.cancelWorkflow(envName)
    }

    def findEnv(envName : String) : Either[RR, Environment] = {
        val env = storeService.findEnv(envName)

        if (env.isDefined)
            Right(env.get)
        else
            Left(RR(isNotFound = true, serviceErrors = Map(RR.envName -> "Failed to find environment with name '%s'".format(envName))))
    }

    def validateEnvName(envName: String) = envName match {
        case envNameRegex(name) => storeService.findEnv(name) match {
            case Some(_) => Some(RR(serviceErrors = Map(RR.envName -> "Environment with the same name already exists")))
            case None => None
        }
        case _ => Some(RR(serviceErrors = Map(RR.envName -> "Environment name must only contains from 3 to 64 lowercase alphanumerics")))
    }
}

object RequestBrokerImpl {
    val envNameRegex = """^([a-zA-Z0-9]\w{2,63})$""".r

    def validateWorkflow(workflow : service.WorkflowDefinition, variables: Map[String, Any], envName: String) = {
        val validationResults = workflow.validate(variables, Option(envName))

        if (!validationResults.isEmpty)
            Some(toRequestResult(validationResults))
        else
            None
    }

    def validateCreator(creator: String) = {
        if (creator != null && creator.trim.length > 0)
            None
        else
            Some(RR(isSuccess = false, compoundServiceErrors = Seq("Creator not found")))
    }

    def toRequestResult(errors : Seq[ValidationError]) = {
        val (varErrors, servErrors) = errors.partition(e => Option(e.variableName).isDefined)
        val variablesErrors = varErrors.map(e => (e.variableName, e.description))
        RR(variablesErrors = variablesErrors.toMap, serviceErrors = servErrors.map((RR.template -> _.description)).toMap)
    }
}