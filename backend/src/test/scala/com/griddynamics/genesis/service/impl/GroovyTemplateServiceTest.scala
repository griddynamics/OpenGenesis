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

    private def testTemplate = templateService.findTemplate(0, "TestEnv", "0.1").get

    @Test def testEmbody() {
        val res = testTemplate.createWorkflow.embody(Map("nodesCount" -> "666", "test" -> "test"))
        assert(res.size === 2)
        assert(res.head.phase == "provision")
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
        assert(res.head === ("TestEnv", "0.1"))
    }

    @Test def testDescribeTemplate() {
        val res = testTemplate
        assert(res.listWorkflows.size === 2)
        assert(res.listWorkflows.filter(_.name == "create").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "nodesCount").headOption.isDefined)
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optional").headOption.get.defaultValue == "1")
        assert(res.createWorkflow.variableDescriptions.filter(_.name == "optionalNoValue").headOption.get.isOptional)
    }

    @Test def testOneOfVariable() {
        val template = testTemplate
        var validate = template.createWorkflow.validate(Map("nodesCount" -> 1, "list" -> 2, "test" -> "test"))
        validate = template.createWorkflow.validate(Map("nodesCount" -> 1, "test" -> "test", "list" -> 10))
        assert(validate.isDefinedAt(0))
        assert(validate(0).variableName == "list")
    }

    @Test def testOneOfDS() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS12 = varDesc.find(_.name == "listDS12")
        assert(listDS12.isDefined)
        expect(Map("value1"->"value1", "value2" -> "value2", "value3" -> "value3"))(listDS12.get.values)
    }

    @Test def testIndependentDataSource() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "listDS1")
        assert(listDS1.isDefined)
        expect(Seq("value1", "value2").map(v => (v,v)).toMap)(listDS1.get.values)
    }

    @Test def testDependent() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "dependent")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("nodesCount" -> 1))
        val descAfterApply = partial.find(_.name == "dependent")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("11" -> "1", "31" -> "3", "41" -> "4"))(descAfterApply.get.values)
    }

    @Test def testDoubleDependent() {
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "doubleDep")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("nodesCount" -> 1, "dependent" -> "z"))
        val descAfterApply = partial.find(_.name == "doubleDep")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("1 < nc:1 < dp:z" -> "1", "3 < nc:1 < dp:z" -> "3", "4 < nc:1 < dp:z" -> "4"))(descAfterApply.get.values)
    }

    @Test def testTripleDependent() {
        implicit val projectId: Int = 1
        val template = testTemplate
        val varDesc =  template.createWorkflow.variableDescriptions
        assert(varDesc.nonEmpty)
        val listDS1 = varDesc.find(_.name == "triple")
        assert(listDS1.isDefined)
        val S1 = Map()
        expect(S1)(listDS1.get.values)
        val partial: Seq[VariableDescription] = template.createWorkflow.partial(Map("list" -> 13, "nodesCount" -> 1, "dependent" -> 'z'))
        assert(partial.length == 3) // three calls of getData, since there is three "parent" variables
        val descAfterApply = partial.find(_.name == "triple")
        assert(descAfterApply.isDefined)
        assert(! descAfterApply.get.values.isEmpty)
        expect(Map("1<13<1<z" -> "1", "3<13<1<z" -> "3", "4<13<1<z" -> "4"))(descAfterApply.get.values)
    }
}



class DependentListDataSource extends ListVarDataSource with DependentDataSource {
    override def getData(param: Any) = values.map(_ + param.toString).zip(values).toMap
    def getData(nodesCount: Any, dependent: Any) = values.map(_ + " < nc:%s".format(nodesCount) + " < dp:%s".format(dependent)).zip(values).toMap
   /*
    *  A method for triple dependent variable. It has three arguments, but
    *  you don't have to declare parameters as Any since you're providing a
    *  correct data types as input
    */
    def getData(list: /*String*/Int, nodesCount: Int, dependent: Char)
    = values.map(_ + "<%s".format(list) + "<%d".format(nodesCount) + "<%s".format(dependent)).zip(values).toMap
    override def getData = Map()
}

class DependentListVarDSFactory extends DataSourceFactory {
    val mode = "dependentList"
    def newDataSource = new DependentListDataSource
}