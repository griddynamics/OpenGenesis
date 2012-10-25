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

import com.griddynamics.genesis.model._
import com.griddynamics.genesis.api.{RequestResult => RR, ExtendedResult}
import com.griddynamics.genesis.service
import service._
import com.griddynamics.genesis.validation.Validation
import java.sql.Timestamp
import scala.Left
import com.griddynamics.genesis.api.Failure
import scala.Some
import scala.Right
import service.ValidationError
import com.griddynamics.genesis.api.Success

trait RequestBroker {
    def createEnv(projectId: Int, envName: String, envCreator : String,
                  templateName: String, templateVersion: String,
                  variables: Map[String, String]) : ExtendedResult[Int]

    def requestWorkflow(envId: Int, projectId: Int, workflowName: String, variables: Map[String, String], startedBy: String) : ExtendedResult[Int]

    def destroyEnv(envId: Int, projectId: Int, variables: Map[String, String], startedBy: String) : ExtendedResult[Int]

    def cancelWorkflow(envId : Int, projectId: Int)

    def resetEnvStatus(envId: Int, projectId: Int) : ExtendedResult[Int]
}

class RequestBrokerImpl(storeService: StoreService,
                        templateService: TemplateService,
                        dispatcher: RequestDispatcher) extends RequestBroker {
    import RequestBrokerImpl._

    def createEnv(projectId: Int, envName: String, envCreator : String,
                  templateName: String, templateVersion: String,
                  variables: Map[String, String]) : ExtendedResult[Int] = {
        validateEnvName(envName, projectId) ++ validateCreator(envCreator) match {
            case f: Failure => return f
            case _ =>
        }

        val twf = templateService.findTemplate(projectId, templateName, templateVersion) match  {
            case None => return Failure(compoundVariablesErrors = Seq("Template %s with version %s not found".format(templateName, templateVersion)),
                isSuccess = false, isNotFound = true)
            case Some(template) => template.getValidWorkflow(template.createWorkflow.name) match {
                case Success(w, _) => w
                case f: Failure => return f
            }
        }

        validateWorkflow(twf, variables, None, projectId) match {
            case f: Failure => return f
            case Success(w, _) =>
        }

        val env = new Environment(envName, EnvStatus.Busy, envCreator,
                            new Timestamp(System.currentTimeMillis()), None, None, templateName, templateVersion, projectId)
        val workflow = new Workflow(env.id, twf.name, envCreator,
                                    WorkflowStatus.Requested, 0, 0, variables, varsDesc(variables, twf), None, None)

        storeService.createEnv(env, workflow) match {
            case Left(m) => Failure(compoundVariablesErrors = Seq(m.toString))
            case Right(_) => {
                dispatcher.createEnv(env.id, env.projectId)
                Success(env.id)
            }
        }
    }

    def destroyEnv(envId: Int, projectId: Int, variables: Map[String, String], startedBy: String) : ExtendedResult[Int] =
        findEnv(envId, projectId).flatMap(env =>  {
            getTemplate(env).flatMap(t => {
                validStart(t, t.destroyWorkflow.name, env, variables, projectId, startedBy)
            })
        })

    def requestWorkflow(envId: Int, projectId: Int, workflowName: String,
                        variables: Map[String, String], startedBy: String) : ExtendedResult[Int] =
        findEnv(envId, projectId).flatMap({env => {
            getTemplate(env).flatMap(t => {
                if (t.createWorkflow.name == workflowName)
                    Failure(serviceErrors = Map(RR.envName ->
                      "It's not allowed to execute create workflow['%s'] in existing environment '%s'"
                        .format(workflowName, env.name)))
                else
                    validStart(t, workflowName, env, variables, projectId, startedBy)
            })
        }})

    private def getTemplate(env: Environment) = templateService.findTemplate(env.projectId, env.templateName, env.templateVersion) match {
        case Some(t) => Success(t)
        case None => Failure(compoundVariablesErrors = Seq("Template used to create environment %s is not found".format(env.name)))
    }

    private def validStart(template: TemplateDefinition, workflowName: String, env: Environment, variables: Map[String, String], projectId: Int, startedBy: String): ExtendedResult[Int] = {
        template.getValidWorkflow(workflowName)
          .flatMap(validateWorkflow(_, variables, Some(env.id), projectId))
          .flatMap(startWorkflow(env, startedBy, variables, _))
    }

    def startWorkflow(env: Environment, startedBy: String, variables: Map[String, String], w: WorkflowDefinition): ExtendedResult[Int] = {
        val workflow = new Workflow(env.id, w.name, startedBy, WorkflowStatus.Requested, 0, 0, variables, varsDesc(variables, w), None, None)
        storeService.requestWorkflow(env, workflow) match {
            case Left(m) => Failure(compoundVariablesErrors = Seq(m.toString))
            case Right((e, wf)) => {
                dispatcher.startWorkflow(e.id, env.projectId)
                Success(e.id)
            }
        }
    }

    def varsDesc(variables: Map[String, String], workflow: WorkflowDefinition) : Map[String, String] = {
        def findDataLabel(k: String, v: String, descriptions: Seq[VariableDescription]): Option[(String, String)] = {
            descriptions.find(_.name == k).flatMap(dsc => {
                dsc.values.find(_._2 == v)
            })
        }
        for ((k, v) <- variables) yield {
            val descriptions: Seq[VariableDescription] = workflow.variableDescriptions
            val value = findDataLabel(k,v,descriptions).getOrElse(findDataLabel(k,v,workflow.partial(variables)).getOrElse((v,v)))._1
            val desc: Option[VariableDescription] = descriptions.find(_.name == k)
            (desc.map(_.description).getOrElse(k), value)
        }
    }

    def cancelWorkflow(envId : Int, projectId: Int) {
        dispatcher.cancelWorkflow(envId, projectId)
    }

    def resetEnvStatus(envId: Int, projectId: Int) : ExtendedResult[Int] = findEnv(envId, projectId).flatMap(env => {
        env.status match {
            case EnvStatus.Broken => {
                storeService.resetEnvStatus(env) match {
                    case Some(m) => Failure(compoundServiceErrors = Seq(m.toString))
                    case _ => Success(envId, isSuccess = true)
                }
            }
            case _ => Failure(compoundServiceErrors = Seq("Environment is not in 'Broken' state"))
        }
    })

    def findEnv(envId : Int, projectId: Int) : ExtendedResult[Environment] = {
        val env = storeService.findEnv(envId, projectId)
        if (env.isDefined)
            Success(env.get)
        else
            Failure(isNotFound = true, serviceErrors = Map(RR.envName -> "Failed to find environment with id '%s'".format(envId)))
    }

    def validateEnvName(envName: String, projectId: Int) = envName match {
        case Validation.projectEnvNamePattern(name) => storeService.findEnv(name, projectId) match {
            case Some(_) => Failure(serviceErrors = Map(RR.envName -> "Environment with the same name already exists in project [id = %s]".format(projectId)))
            case None => Success((envName, projectId))
        }
        case _ => Failure(serviceErrors = Map(RR.envName -> Validation.projectEnvNameErrorMessage))
    }
}

object RequestBrokerImpl {
    def validateWorkflow(workflow : service.WorkflowDefinition, variables: Map[String, Any], envId: Option[Int], projectId: Int) = {
        val validationResults = workflow.validate(variables, envId, Option(projectId))

        if (!validationResults.isEmpty)
            toFailure(validationResults)
        else
            Success(workflow)
    }

    def validateCreator(creator: String) = {
        if (creator != null && creator.trim.length > 0)
            Success(creator)
        else
            Failure(isSuccess = false, compoundServiceErrors = Seq("Creator not found"))
    }

    def toFailure(errors : Seq[ValidationError]) = {
        val (varErrors, servErrors) = errors.partition(e => Option(e.variableName).isDefined)
        val variablesErrors = varErrors.map(e => (e.variableName, e.description))
        Failure(variablesErrors = variablesErrors.toMap, serviceErrors = servErrors.map((RR.template -> _.description)).toMap)
    }
}