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
import org.springframework.core.convert.support.ConversionServiceFactory
import com.griddynamics.genesis.template.{ListVarDSFactory, VersionedTemplate, TemplateRepository}
import org.junit.{Before, Test}
import com.griddynamics.genesis.core.{RegularWorkflow, GenesisFlowCoordinator}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.model.{WorkflowStep, Workflow, Environment}
import com.griddynamics.genesis.model.WorkflowStepStatus._
import com.griddynamics.genesis.workflow.{Step, StepResult}
import com.griddynamics.genesis.plugin.{GenesisStep, GenesisStepResult, StepCoordinatorFactory}
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.repository.DatabagRepository

class GroovyTemplateContextTest extends AssertionsForJUnit with MockitoSugar {
  val templateRepository = mock[TemplateRepository]

  val storeService = {
    val storeService = mock[StoreService]
    when(storeService.startWorkflow(Matchers.any(), Matchers.any())).thenReturn((mock[Environment], mock[Workflow], List()))
    when(storeService.insertWorkflowStep(Matchers.any())).thenReturn(
      new WorkflowStep(workflowId = IdGen.generate, phase = "", status = Requested, details = "", started = None, finished = None )
    )
    storeService
  }

  @Before def setUp() {
    CacheManager.getInstance().clearAll()
  }

  val stepCoordinatorFactory = mock[StepCoordinatorFactory]
  val databagRepository = mock[DatabagRepository]
  val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ContextExample.genesis"))
  Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))

  val templateService = new GroovyTemplateService(templateRepository,
    List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
    Seq(new ListVarDSFactory, new DependentListVarDSFactory), databagRepository, CacheManager.getInstance())

  @Test def contextVariableAccess() {
    val stepBuilders = templateService.findTemplate(0, "TestEnv", "0.1").get.createWorkflow.embody(Map())
    stepBuilders.foreach { _.id = IdGen.generate }

    val flowCoordinator = new GenesisFlowCoordinator(0, 0, stepBuilders, storeService, stepCoordinatorFactory) with RegularWorkflow

    flowCoordinator.onFlowStart()

    val firstStepResult = {
      val firstStep = stepBuilders(0).newStep
      new GenesisStepResult(firstStep, actualResult = Some(new ComplexStepResult(firstStep)))
    }

    flowCoordinator.onStepFinish(firstStepResult) match {
      case Right(head :: _) => {
        assert (head.step.asInstanceOf[GenesisStep].actualStep.asInstanceOf[DoNothingStep].name === ActualResultObject.text)
      }
      case _ => fail ("flow coodinator expected to return second step execution coodinator")
    }
  }

  @Test def detailsDescription() {
    val builders = templateService.findTemplate(0, "TestEnv", "0.1").get.createWorkflow.embody(Map())
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