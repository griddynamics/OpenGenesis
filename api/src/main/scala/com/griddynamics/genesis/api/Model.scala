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
package com.griddynamics.genesis.api

case class Environment(name : String,
                       status : String,
                       completed : Option[Double] = None, //optional field, only if status Executing(workflow)
                       creator : String,
                       templateName : String,
                       templateVersion : String,
                       projectId: Int)

case class EnvironmentDetails(name : String,
                              status : String,
                              creator : String,
                              templateName : String,
                              templateVersion : String,
                              workflows : Seq[Workflow],
                              createWorkflowName : String,
                              destroyWorkflowName : String,
                              vms : Seq[VirtualMachine],
                              workflowsHistory : Option[Seq[WorkflowDetails]] = None)

case class Variable(name : String, description : String, optional: Boolean = false, defaultValue: String = null)

case class Template(name : String, version : String, createWorkflow : Workflow)

case class Workflow(name : String, variables : Seq[Variable])

case class WorkflowStep(stepId: String, phase: String, status : String, details : String)

case class WorkflowDetails(name : String, status: String,  stepsCompleted: Option[Double], steps : Option[Seq[WorkflowStep]])

case class VirtualMachine(envName : String,
                          roleName : String,
                          hostNumber : Int,
                          instanceId : String,
                          hardwareId : String,
                          imageId : String,
                          publicIp : String,
                          privateIp : String,
                          status : String)

case class RequestResult(serviceErrors : Map[String, String] = Map(),
                         variablesErrors : Map[String, String] = Map(),
                         compoundServiceErrors : Seq[String] = Seq(),
                         compoundVariablesErrors : Seq[String] = Seq(),
                         isSuccess : Boolean = false,
                         isNotFound : Boolean = false)  {
    def hasValidationErrors = ! isSuccess && (! variablesErrors.isEmpty || ! serviceErrors.isEmpty)
    def ++(other: RequestResult) : RequestResult = {
        RequestResult(serviceErrors = this.serviceErrors ++ other.serviceErrors,
            variablesErrors = this.variablesErrors ++ other.variablesErrors,
            compoundServiceErrors = this.compoundServiceErrors ++ other.compoundServiceErrors,
            compoundVariablesErrors = this.compoundVariablesErrors ++ other.compoundVariablesErrors,
            isSuccess = this.isSuccess && other.isSuccess,
            isNotFound = this.isNotFound && other.isNotFound
        )
    }
}

case class User(username: String, email: String, firstName: String, lastName: String, jobTitle: Option[String], password: Option[String]) {

}

case class Project(id: Option[Int], name: String,  description: Option[String], projectManager: String)

case class ConfigProperty(name: String, value: String)

case class Plugin(id: String, description: Option[String])

case class PluginDetails(id: String,  description: Option[String], configuration: Map[String, Any]);

object RequestResult {
    val envName = "envName"
    val template = "template"
}

case class VcsProject(name : String, id: String)
case class Tag(p: VcsProject, name: String)
