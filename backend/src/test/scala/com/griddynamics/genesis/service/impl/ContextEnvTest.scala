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
package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.util.IoUtil
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.service.Builders
import com.griddynamics.genesis.template.ListVarDSFactory
import org.junit.Test
import com.griddynamics.genesis.core.{RegularWorkflow, GenesisFlowCoordinator}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.api
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.model.WorkflowStepStatus._
import com.griddynamics.genesis.plugin.StepCoordinatorFactory
import java.sql.Timestamp
import java.util.Date
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.cache.NullCacheManager

class ContextEnvTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse {

  val env = new Environment("test_env", EnvStatus.Ready, "creator", new Timestamp(new Date().getTime), None, None, "", "", 0, 0)

  val storeService = {
    val storeService = mock[StoreService]
    when(storeService.startWorkflow(Matchers.any(), Matchers.any())).thenReturn((env, mock[Workflow], List()))
    when(storeService.insertWorkflowStep(Matchers.any())).thenReturn(
      new WorkflowStep(workflowId = 0, phase = "", status = Requested, details = "", started = None, finished = None )
    )
    when(storeService.findWorkflow(Matchers.anyInt())).thenReturn(Option(mock[Workflow]))
    storeService
  }

  when(configService.getDefault(Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))
  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  val stepCoordinatorFactory = mock[StepCoordinatorFactory]
  val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ContextEnv.genesis"))
  Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))

  val templateService = new GroovyTemplateService(templateRepoService,
    List(new DoNothingStepBuilderFactory), new DefaultConversionService,
    Seq(new ListVarDSFactory, new DependentListVarDSFactory), databagRepository, configService, NullCacheManager)

  val TEST_ENV_ATTR = EntityAttr[String]("test_attr")
  val TEST_ENV_VAL = "test_attr_value"

  private lazy val template = templateService.findTemplate(0, "ContextWithEnv", "0.1", 1).get

  private def flowCoordinator(stepBuilders: Builders) = new GenesisFlowCoordinator(0, 0, stepBuilders.regular,
    storeService, stepCoordinatorFactory, stepBuilders.onError) with RegularWorkflow

  @Test def contextEnvAccess() {
    flowCoordinator(template.createWorkflow.embody(Map())).onFlowStart() match {
      case Right(head :: _) => {
        assert (head.step.asInstanceOf[GenesisStep].actualStep.asInstanceOf[DoNothingStep].name === env.name)
      }
      case _ => fail ("create flow coordinator expected to return first step execution coordinator")
    }
   }

  @Test def contextEnvAttrAccess() {
    env(TEST_ENV_ATTR) = TEST_ENV_VAL
    flowCoordinator(template.destroyWorkflow.embody(Map())).onFlowStart() match {
      case Right(head :: _) => {
        assert (head.step.asInstanceOf[GenesisStep].actualStep.asInstanceOf[DoNothingStep].name === env(TEST_ENV_ATTR))
      }
      case _ => fail ("destroy flow coordinator expected to return first step execution coordinator")
    }
   }

  @Test def contextEnvPropMissing() {
    flowCoordinator(template.getValidWorkflow("test").get.embody(Map())).onFlowStart match {
      case Right(head :: _) => {
        assert (head.step.asInstanceOf[GenesisStep].actualStep.asInstanceOf[DoNothingStep].name === null)
      }
      case _ => fail ("test flow coordinator expected to return first step execution coordinator")
    }
   }
}
