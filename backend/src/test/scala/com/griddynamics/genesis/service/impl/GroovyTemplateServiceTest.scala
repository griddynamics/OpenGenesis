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
package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import org.mockito.Mockito
import com.griddynamics.genesis.plugin._
import reflect.BeanProperty
import org.springframework.core.convert.support.ConversionServiceFactory
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.service.VariableDescription
import com.griddynamics.genesis.template._

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

class GroovyTemplateServiceTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ExampleEnv.genesis"))
    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    val templateService = new GroovyTemplateService(templateRepository,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory))

    @Test def testEmbody() {
        val res = templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.embody(Map("nodesCount" -> "1", "test" -> "test"))
        assert(res.size === 2)
        assert(res.head.phase == "provision")

        templateService.findTemplate("TestEnv", "0.1").get.destroyWorkflow.embody(Map())
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testEmbodyWrongVariableCount() {
        templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.embody(Map("nodesCount" -> "1"))
    }

    @Test def testValidateNoVariable() {
        val res = templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.validate(Map())
        assert(res.size === 2)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidateWrongVariable() {
        val res = templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.validate(Map("nodesCount" -> "nothing", "test" -> "test"))
        assert(res.size === 1)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidationError() {
        val res = templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.validate(Map("nodesCount" -> "0", "test" -> "test"))
        assert(res.size === 1)
        assert(res.head.variableName === "nodesCount")
    }

    @Test def testValidate() {
        val res = templateService.findTemplate("TestEnv", "0.1").get.createWorkflow.validate(Map("nodesCount" -> "1", "test" -> "test"))
        assert(res.isEmpty)
    }

    @Test def testListTemplates() {
        val res = templateService.listTemplates
        assert(res.size === 1)
        assert(res.head === ("TestEnv", "0.1"))
    }

    @Test def testDescribeTemplate() {
        val res = templateService.findTemplate("TestEnv", "0.1").get
        assert(res.listWorkflows.size === 2)
        assert(res.listWorkflows.filter(_.name == "create").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "nodesCount").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optional").headOption.get.defaultValue == "1")
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optionalNoValue").headOption.get.isOptional)
    }

    @Test def testOneOfVariable() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        var validate = template.createWorkflow.validate(Map("nodesCount" -> 1, "list" -> 2, "test" -> "test"))
        validate = template.createWorkflow.validate(Map("nodesCount" -> 1, "test" -> "test", "list" -> 10))
        assert(validate.isDefinedAt(0))
        assert(validate(0).variableName == "list")
    }

    @Test def testOneOfDS() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS12 = varDesc.find(_.name == "listDS12")
        assert(listDS12.isDefined)
        expect(Seq("value1", "value2", "value3"))(listDS12.get.values)
    }

    @Test def testIndependentDataSource() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "listDS1")
        assert(listDS1.isDefined)
        expect(Seq("value1", "value2"))(listDS1.get.values)
    }

    @Test def testDependent() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "dependent")
        assert(listDS1.isDefined)
        val S1 = Seq()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("nodesCount" -> 1))
        val descAfterApply = partial.find(_.name == "dependent")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Seq("11", "31", "41"))(descAfterApply.get.values)
    }

    @Test def testDoubleDependent() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "doubleDep")
        assert(listDS1.isDefined)
        val S1 = Seq()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("nodesCount" -> "x", "dependent" -> "z"))
        val descAfterApply = partial.find(_.name == "doubleDep")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Seq("1 < nc:x < dp:z", "3 < nc:x < dp:z", "4 < nc:x < dp:z"))(descAfterApply.get.values)
    }

    @Test def testTripleDependent() {
        val template = templateService.findTemplate("TestEnv", "0.1").get
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "triple")
        assert(listDS1.isDefined)
        val S1 = Seq()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("list" -> "x", "nodesCount" -> "y", "dependent" -> "z"))
        val descAfterApply = partial.find(_.name == "triple")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Seq("1<x<y<z", "3<x<y<z", "4<x<y<z"))(descAfterApply.get.values)
    }
}



class DependentListDataSource extends ListVarDataSource with DependentDataSource {
    def getData(param: Any) = values.map(_ + param.toString).toSeq
    def getData(nodesCount: Any, dependent: Any) = values.map(_ + " < nc:%s".format(nodesCount) + " < dp:%s".format(dependent)).toSeq
    def getData(list: Any, nodesCount: Any, dependent: Any)
    = values.map(_ + "<%s".format(list) + "<%s".format(nodesCount) + "<%s".format(dependent)).toSeq
}

class DependentListVarDSFactory extends DataSourceFactory {
    val mode = "dependentList"
    def newDataSource = new DependentListDataSource
}