/*
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
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.plugin._
import reflect.BeanProperty
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.cache.NullCacheManager
import org.mockito.Mockito._
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

case class DoNothingStep(name: String) extends Step {
  override def stepDescription = "Best step ever!"
}

class DoNothingStepBuilderFactory extends StepBuilderFactory {
    val stepName = "teststep"

    def newStepBuilder = new StepBuilder {
        @BeanProperty var text: String = _

        def getDetails = DoNothingStep(text)
    }
}

class GroovyTemplateServiceTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse {
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ExampleEnv.genesis"))
    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(templateRepository.getContent(VersionedTemplate("1") )).thenReturn(Some(body))

    Mockito.when(configService.getDefault(Matchers.anyInt)).thenReturn(None)
    Mockito.when(configService.list(Matchers.anyInt)).thenReturn(Seq())
    when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(), databagRepository, configService, NullCacheManager)

    private def testTemplate = templateService.findTemplate(0, "TestEnv", "0.1", 1).get

    @Test def testEmbody() {
        val res = testTemplate.createWorkflow.embody(Map("nodesCount" -> "666", "test" -> "test"))
        assert(res.regular.size === 2)
        assert(res.regular.head.phase == "provision")
        assert(res.apply(0).getDetails.asInstanceOf[DoNothingStep].name === "666")
        testTemplate.destroyWorkflow.embody(Map())
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testEmbodyWrongVariableCount() {
        testTemplate.createWorkflow.embody(Map("nodesCount" -> "1"))
    }

    @Test def testValidateNoVariable() {
        val res = testTemplate.createWorkflow.validate(Map())
        assert(res.size === 2)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidateWrongVariable() {
        val res = testTemplate.createWorkflow.validate(Map("nodesCount" -> "nothing", "test" -> "test"))
        assert(res.size === 1)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidationError() {
        val res = testTemplate.createWorkflow.validate(Map("nodesCount" -> "0", "test" -> "test"))
        assert(res.size === 1)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidate() {
        val res = testTemplate.createWorkflow.validate(Map("nodesCount" -> "1", "test" -> "test"))
        assert(res.isEmpty)
    }

    @Test def testListTemplates() {
        val res = templateService.listTemplates(0)
        assert(res.size === 1)
        assert(res.head.name === "TestEnv")
        assert(res.head.version === "0.1")
    }

    @Test def testDescribeTemplate() {
        val res = testTemplate
        assert(res.listWorkflows.size === 2)
        assert(res.listWorkflows.filter(_.name == "create").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "nodesCount").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optional").headOption.get.defaultValue == "1")
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optionalNoValue").headOption.get.isOptional)
    }

}