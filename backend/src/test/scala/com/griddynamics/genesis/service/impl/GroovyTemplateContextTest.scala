/*
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
package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.util.IoUtil
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.template.{ListVarDSFactory, VersionedTemplate}
import org.junit.Test
import com.griddynamics.genesis.core.{RegularWorkflow, GenesisFlowCoordinator}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.model.{WorkflowStep, Workflow, Environment}
import com.griddynamics.genesis.model.WorkflowStepStatus._
import com.griddynamics.genesis.workflow.{Step, StepResult}
import com.griddynamics.genesis.plugin.{GenesisStep, GenesisStepResult, StepCoordinatorFactory}
import com.griddynamics.genesis.api
import com.griddynamics.genesis.cache.NullCacheManager

class GroovyTemplateContextTest extends AssertionsForJUnit with MockitoSugar  with DSLTestUniverse {

  val storeService = {
    val storeService = mock[StoreService]
    when(storeService.startWorkflow(Matchers.any(), Matchers.any())).thenReturn((mock[Environment], mock[Workflow], List()))
    when(storeService.insertWorkflowStep(Matchers.any())).thenReturn(
      new WorkflowStep(workflowId = IdGen.generate, phase = "", status = Requested, details = "", started = None, finished = None )
    )
    when(storeService.findWorkflow(Matchers.anyInt())).thenReturn(Option(mock[Workflow]))
    storeService
  }

  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  val stepCoordinatorFactory = mock[StepCoordinatorFactory]
  val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ContextExample.genesis"))
  Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))

  val templateService = new GroovyTemplateService(templateRepoService,
    List(new DoNothingStepBuilderFactory), new DefaultConversionService,
    Seq(new ListVarDSFactory, new DependentListVarDSFactory), databagRepository, configService, NullCacheManager)

  @Test def contextVariableAccess() {
    val stepBuilders = templateService.findTemplate(0, "TestEnv", "0.1", 0).get.createWorkflow.embody(Map())
    stepBuilders.regular.foreach { _.id = IdGen.generate }

    val flowCoordinator = new GenesisFlowCoordinator(0, 0, stepBuilders.regular, storeService, stepCoordinatorFactory, stepBuilders.onError) with RegularWorkflow

    flowCoordinator.onFlowStart()

    val firstStepResult = {
      val firstStep = stepBuilders.regular(0).newStep
      new GenesisStepResult(firstStep, actualResult = Some(new ComplexStepResult(firstStep)))
    }

    flowCoordinator.onStepFinish(firstStepResult) match {
      case Right(head :: _) => {
        assert (head.step.asInstanceOf[GenesisStep].actualStep.asInstanceOf[DoNothingStep].name === ActualResultObject.text)
      }
      case _ => fail ("flow coordinator expected to return second step execution coordinator")
    }
  }

  @Test def detailsDescription() {
    val builders = templateService.findTemplate(0, "TestEnv", "0.1", 0).get.createWorkflow.embody(Map())
    assert(builders(0).getDetails.stepDescription === new DoNothingStep("").stepDescription)
    assert(builders(1).getDetails.stepDescription === UninitializedStepDetails.stepDescription)
  }

}

class ComplexStepResult(val step: Step) extends StepResult {
  val description = ActualResultObject
}

object ActualResultObject {
  val text = "complext text"
}
object IdGen {
  var id = 0
  def generate = { id += 1; id }
}