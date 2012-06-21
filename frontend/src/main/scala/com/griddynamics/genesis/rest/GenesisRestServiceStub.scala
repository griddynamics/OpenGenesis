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
package com.griddynamics.genesis.rest

import com.griddynamics.genesis.api._
import scala.Some
import java.sql.Timestamp

class GenesisRestServiceStub() extends GenesisService {
    val createWorkflow = Workflow("create",
        Seq(Variable("creator", "Contact Person"),
            Variable("nodes", "Nodes Count")))

    val destroyWorkflow = Workflow("destroy", Seq())

    val testWorkflow = Workflow("test-workflow",
                                Seq(Variable("first",  "First"),
                                    Variable("second", "Second"),
                                    Variable("third",  "Third")))

    val workflows = Seq(createWorkflow, destroyWorkflow, testWorkflow)

    val envT = Environment("name", "Requested", None, "artem", "erlangEnv", "1.0.0", 1)
    val vmT = VirtualMachine("environment", "erlangNode", 1, "instanceId", "hardwareId", "hostNumber", "publicIp", "privateIp", "Ready")

    val templateT = Template("erlangEnv", "1.0.0", createWorkflow)

    val envDescT =
        EnvironmentDetails(
            "name",
            "Requested",
            "artem",
            "erlangEnv",
            "1.0.0",
            workflows,
            "create",
            "destroy",
            Seq(vmT, vmT, vmT),
            1
        )

    var envs = Seq(
        envT.copy(name = "test-env1"),
        envT.copy(name = "test-env2"),
        envT.copy(name = "test-env3"),
        envT.copy(name = "test-env4"),
        envT.copy(name = "test-env5")
    )

    val envsDescr = Map(
        "test-env1" -> envDescT.copy(name = "test-env1"),
        "test-env2" -> envDescT.copy(name = "test-env2"),
        "test-env3" -> envDescT.copy(name = "test-env3"),
        "test-env4" -> envDescT.copy(name = "test-env4"),
        "test-env5" -> envDescT.copy(name = "test-env5")
    )

    val templates = Seq(templateT, templateT.copy(version = "1.0.1"), templateT.copy(version = "1.0.2"))

    def createEnv(projectId: Int, name: String, creator : String, templateName: String, templateVersion: String, vars: Map[String, String]) = {
        /*
        RequestResult(serviceErrors : Map[String, String] = Map(),
                         variablesErrors : Map[String, String] = Map(),
                         compoundServiceErrors : Seq[String] = Seq(),
                         compoundVariablesErrors : Seq[String] = Seq(),
                         isSuccess : Boolean = false)
        */
        name match {
            case "error" => RequestResult(isSuccess = false,
                                          serviceErrors = Map(),
                                          variablesErrors = Map("nodes" -> "Invalid nodes count",
                                                                "creator" -> "Invalid creator"),
                                          compoundServiceErrors = Seq("CompoundServiceErrors1",
                                                                      "CompoundServiceErrors2"),
                                          compoundVariablesErrors = Seq("CompoundVariablesErrors1",
                                                                        "CompoundVariablesErrors2"))
            case _ => val newEnv = Environment(name, "Executing(create)",
                                               Some(0.0), creator,
                                               templateName, templateVersion, projectId)
                      envs =  newEnv +: envs
                      RequestResult(isSuccess = true)
        }
    }

    def destroyEnv(envName: String, vars: Map[String, String]) = {
        envs = envs.filter(e => e.name != envName)
        RequestResult(isSuccess = true)
    }

    def countEnvs(projectId: Int) = envs.size

    def listTemplates(projectId: Int) = templates

    def describeEnv(envName : String) = envsDescr.get(envName)

    def listEnvs(projectId: Int) = {
        envs = envs map {e =>
            if(e.status.startsWith("Executing")){
                val completed = e.completed.get
                if(completed < 1.0) e.copy(completed = Some(completed + 0.1))
                else e.copy(status = "Success", completed = None)
            } else e
        }

        envs
    }

    def listEnvs(projectId: Int, start : Int, limit : Int) = listEnvs(projectId)

    def requestWorkflow(envName: String, workflowName: String, variables: Map[String, String]) = {
        envs = envs map {e =>
            if(e.name == envName)
                e.copy(status = "Executing(%s)".format(workflowName), completed = Some(0.0))
            else e
        }

        RequestResult(isSuccess = true)
    }

    def cancelWorkflow(envName: String) {}
    
    def listProjects() = Map("1" -> "2")
    
    def listTags(projectId: String) = Seq("tag1", "tag2")
    
    def listTemplates(projectId : String, tagId : String) = Seq(templateT)
    
    def listTemplates(projectId: String) = Seq(templateT)

    def getLogs(envName: String, stepId: Int) = Seq("aaa", "bbb")

    def isEnvExists(envName: String, projectId: Int) = true

    def queryVariables(projectId: Int, templateName: String, templateVersion: String, workflow: String, variables: Map[String, String]) = Some(Seq())

    def getTemplate(projectId: Int, templateName: String, templateVersion: String) = listTemplates(0).headOption

    def getStepLog(stepId: Int) = List(new ActionTracking("test", Option("test descriptuion"), System.currentTimeMillis(), None))
}
