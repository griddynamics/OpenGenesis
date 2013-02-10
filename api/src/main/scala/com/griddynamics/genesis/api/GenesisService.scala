/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.api

import java.util.Date

trait GenesisService extends TemplateRestService {
  def updateEnvironmentName(i: Int, projectId: Int, value: String): ExtendedResult[Int]

  def getLogs(envId: Int, stepId: Int, includeActions: Boolean): Seq[StepLogEntry]

  def getLogs(envId: Int, actionUUID: String): Seq[StepLogEntry]

  def listEnvs(projectId: Int, statusFilter: Option[Seq[String]] = None, ordering: Option[Ordering] = None): Seq[Environment]

  def countEnvs(projectId: Int): Int

  def describeEnv(envId: Int, projectId: Int): Option[EnvironmentDetails]

  def workflowHistory(envId: Int, projectId: Int, pageOffset: Int, pageLength: Int): Option[WorkflowHistory]

  def createEnv(projectId: Int, envName: String, creator: String, templateName: String,
                templateVersion: String, variables: Map[String, String], config: Configuration, timeToLive: Option[Long]): ExtendedResult[Int]

  def destroyEnv(envId: Int, projectId: Int, variables: Map[String, String], startedBy: String): ExtendedResult[Int]

  def requestWorkflow(envId: Int, projectId: Int, workflowName: String, variables: Map[String, String], startedBy: String): ExtendedResult[Int]

  def resetEnvStatus(envId: Int, projectId: Int): ExtendedResult[Int]

  def cancelWorkflow(envId: Int, projectId: Int)

  def isEnvExists(envId: Int, projectId: Int): Boolean

  def getStepLog(stepId: Int): Seq[ActionTracking]

  def stepExists(stepId: Int, envId: Int): Boolean

  def updateTimeToLive(projectId: Int, envId: Int, timeToLiveSecs: Long): ExtendedResult[Date]

  def removeTimeToLive(projectId: Int, envId: Int): ExtendedResult[Boolean]

}
