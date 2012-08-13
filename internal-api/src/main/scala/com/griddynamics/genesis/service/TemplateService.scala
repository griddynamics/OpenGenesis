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

class VariableDescription(val name: String, val description: String, val isOptional: Boolean = false,
                          val defaultValue: String = null, val values: Map[String,String] = Map(), val dependsOn: Option[List[String]] = None)

case class ValidationError(variableName: String, description: String)
class ConversionException(val fieldId: String, val message: String) extends Exception

trait TemplateDefinition {
    def createWorkflow: WorkflowDefinition

    def destroyWorkflow: WorkflowDefinition

    def listWorkflows: Seq[WorkflowDefinition]

    def getWorkflow(name: String): Option[WorkflowDefinition]
}

trait WorkflowDefinition {
    def name: String

    def variableDescriptions: Seq[VariableDescription]

    def embody(variables: Map[String, String], envId: Option[Int] = None, projectId: Option[Int] = None): Seq[StepBuilder]

    def validate(variables: Map[String, Any], envId: Option[Int] = None, projectId: Option[Int] = None): Seq[ValidationError]

    def partial(variables: Map[String, Any]): Seq[VariableDescription] = Seq()
}

case class TemplateDescription (name: String, version: String, createWorkflow: String, destroyWorkflow: String, workflows: Seq[String])

trait TemplateService {
    def listTemplates(projectId: Int): Seq[(String, String)] // (name, version)
    def findTemplate(projectId: Int, templateName: String, templateVersion: String): Option[TemplateDefinition]

    def descTemplate(projectId: Int, templateName: String, templateVersion: String): Option[TemplateDescription]
    def templateRawContent(projectId: Int, name: String, version: String): Option[String]
}
