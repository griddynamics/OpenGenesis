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
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import org.springframework.core.convert.support.ConversionServiceFactory
import org.junit.{Before, Test}
import com.griddynamics.genesis.util.IoUtil
import org.mockito.Mockito
import com.griddynamics.genesis.service.{ValidationError, VariableDescription}
import com.griddynamics.genesis.plugin.{StepBuilder, GenesisStep}
import com.griddynamics.genesis.repository.DatabagRepository
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.api.{DataItem, DataBag}
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory

class GroovyTemplateProjectContextTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateService = new GroovyTemplateService(templateRepository,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, CacheManager.getInstance())
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ProjectContextExample.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(bagRepository.findByName("foo", Some(0))).thenReturn(Some(testDatabag))
    Mockito.when(bagRepository.findByName("bar", None)).thenReturn(Some(systemDatabag))
    Mockito.when(bagRepository.findByTags(Seq("bar"), None)).thenReturn(Seq(systemDatabag))
    Mockito.when(bagRepository.findByTags(Seq("foo"), Some(0))).thenReturn(Seq(testDatabag))
    val createWorkflow = templateService.findTemplate(0, "Projects", "0.1").get.createWorkflow

    @Before def setUp() {
      CacheManager.getInstance().clearAll()
    }


    def testDatabag : DataBag = {
        val db = DataBag(Some(0), "foo", Seq("foo"), Some(0), Some(Seq(
            DataItem(Some(0), "key1", "fred", 0),
            DataItem(Some(0), "key2", "barney", 0),
            DataItem(Some(0), "key3", "wilma", 0)
        )))
        db
    }

    def systemDatabag : DataBag = {
        val db = DataBag(Some(0), "bar", Seq("bar"), None, Some(Seq(
            DataItem(Some(0), "key1", "barney", 0),
            DataItem(Some(0), "key2", "wilma", 0),
            DataItem(Some(0), "key3", "fred", 0)
        )))
        db
    }

    @Test def testDefaultValue() {
        val projectVariable: VariableDescription = createWorkflow.variableDescriptions.find(_.name == "projectKey").getOrElse(fail("Variable projectKey must be declared"))
        assert(projectVariable.defaultValue == "barney")
    }

    @Test def testDSConfig() {
        val listVariable: VariableDescription = createWorkflow.variableDescriptions.find(_.name == "projectList").getOrElse(fail("Variable projectList must be declared"))
        assert(listVariable.values == Map("fred" -> "fred"))
    }

    @Test def testProjectDbSource() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("projectdb" -> "foo"))
        assert(validate.isEmpty)
    }

    @Test def testSystemDbSource() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("sysdb" -> "bar"))
        assert(validate.isEmpty)
    }

    @Test def testSystemDbSourceWrongValue() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("sysdb" -> "foo"))
        assert(! validate.isEmpty)
    }

    @Test def testApplyVariable() {
        val builder: StepBuilder = createWorkflow.embody(Map()).head
        val newStep: GenesisStep = builder.newStep
        val actualStep: DoNothingStep = newStep.actualStep.asInstanceOf[DoNothingStep]
        assert(actualStep.name == "wilma")
    }
}
