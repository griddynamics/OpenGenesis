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

import com.griddynamics.genesis.plugin.StepBuilder
import com.griddynamics.genesis.api.{Configuration, Failure, Success, ExtendedResult}
import com.griddynamics.genesis.model.Environment

class VariableDescription(val name: String, val clazz : Class[_ <: Any], val description: String, val isOptional: Boolean = false,
                          val defaultValue: String = null, val values: Option[Map[String,String]] = None, val dependsOn: Option[List[String]] = None,
                          val group: Option[String] = None)

case class ValidationError(variableName: String, description: String)
class ConversionException(val fieldId: String, val message: String) extends Exception

trait TemplateDefinition {
    def createWorkflow: WorkflowDefinition

    def destroyWorkflow: WorkflowDefinition

    def listWorkflows: Seq[WorkflowDefinition]

    def getWorkflow(name: String): Option[WorkflowDefinition]

    def getValidWorkflow(name: String): ExtendedResult[WorkflowDefinition] = getWorkflow(name) match {
        case Some(s) => Success(s)
        case _ => Failure(isNotFound = true)
    }
}

trait WorkflowDefinition {
    def name: String

    def variableDescriptions: Seq[VariableDescription]

    def embody(variables: Map[String, String], envId: Option[Int] = None, projectId: Option[Int] = None): Builders

    def validate(variables: Map[String, Any]) : Seq[ValidationError]

    def validatePreconditions(variables: Map[String, Any], config: Configuration): ExtendedResult[_]

    def partial(variables: Map[String, Any]): Seq[VariableDescription] = Seq()
}

case class Builders(regular: Seq[StepBuilder], onError: Seq[StepBuilder] = Seq()) {
    def apply(index: Int) = regular(index)
}

case class TemplateDescription(name: String, version: String, createWorkflow: String, destroyWorkflow: String, workflows: Seq[String], rawBody: String)

trait TemplateService {
    def listTemplates(projectId: Int): Seq[TemplateDescription]
    def descTemplate(projectId: Int, templateName: String, templateVersion: String): Option[TemplateDescription]

    def findTemplate(projectId: Int, templateName: String, templateVersion: String, envConfId: Int): Option[TemplateDefinition]
    def findTemplate(env: Environment): Option[TemplateDefinition]

    def templateRawContent(projectId: Int, name: String, version: String): Option[String]
    def clearCache(projectId: Int) //TODO: remove this
}


