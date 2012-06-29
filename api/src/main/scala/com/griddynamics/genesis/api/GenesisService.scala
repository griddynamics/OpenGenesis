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

trait GenesisService {
    def queryVariables(projectId: Int, templateName: String, templateVersion: String, workflow: String, variables: Map[String, String]) : Option[Seq[Variable]]

    def getTemplate(projectId: Int, templateName: String, templateVersion: String) : Option[Template]

    def getLogs(envName: String, stepId: Int) : Seq[String]

    def listEnvs (projectId: Int) : Seq[Environment]

    def listEnvs(projectId: Int, start : Int, limit : Int) : Seq[Environment]

    def countEnvs(projectId: Int) : Int

    def describeEnv(envName : String) : Option[EnvironmentDetails]

    def workflowHistory(envName: String, pageOffset: Int, pageLength: Int) : Option[WorkflowHistory]

    def listTemplates(projectId: Int) : Seq[Template]

    def createEnv(projectId: Int, envName : String, creator : String, templateName : String,
                  templateVersion : String, variables : Map[String, String]) : RequestResult

    def destroyEnv(envName : String, variables : Map[String, String]) : RequestResult

    def requestWorkflow(envName : String, workflowName : String, variables : Map[String, String]) : RequestResult

    def cancelWorkflow(envName : String)

    def isEnvExists(envName: String, projectId: Int): Boolean

    def getStepLog(stepId: Int): Seq[ActionTracking]
}
