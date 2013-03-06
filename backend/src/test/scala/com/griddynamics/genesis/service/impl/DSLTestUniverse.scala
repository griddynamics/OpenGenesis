/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.service.impl

import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.template.TemplateRepository
import com.griddynamics.genesis.service.{EnvironmentConfigurationService, TemplateRepoService}
import com.griddynamics.genesis.repository.DatabagRepository
import org.mockito.Mockito
import com.griddynamics.genesis.model.{VariablesField, WorkflowStatus, Workflow, EnvStatus, Environment}
import java.sql.Timestamp
import java.util.Date

trait DSLTestUniverse extends MockitoSugar{
  val templateRepository = mock[TemplateRepository]
  val templateRepoService = mock[TemplateRepoService]
  val databagRepository = mock[DatabagRepository]

  Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
  val configService = mock[EnvironmentConfigurationService]

  val dummyEnv = new Environment("test_env", EnvStatus.Ready, "creator", new Timestamp(new Date().getTime), None, None, "", "", 0, 0)
  val dummyWorkflow = new Workflow(1, "provision", "genesis", WorkflowStatus.Requested, 2, 0, new VariablesField("test"), new VariablesField("test"), None, None )

}
