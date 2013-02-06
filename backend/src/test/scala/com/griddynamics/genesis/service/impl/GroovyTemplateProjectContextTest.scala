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
import com.griddynamics.genesis.service.{EnvironmentService, TemplateRepoService, ValidationError, VariableDescription}
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import org.springframework.core.convert.support.DefaultConversionService
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.plugin.{StepBuilder, GenesisStep}
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.api.{DataItem, DataBag}
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import com.griddynamics.genesis.cache.NullCacheManager

class GroovyTemplateProjectContextTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
  val configService = mock[EnvironmentService]

  Mockito.when(configService.getDefault(Matchers.anyInt)).thenReturn(None)
  Mockito.when(configService.list(Matchers.anyInt)).thenReturn(Seq())

    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory,
        new DatabagDataSourceFactory(bagRepository)), bagRepository, configService, NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/ProjectContextExample.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(bagRepository.findByName("foo", Some(0))).thenReturn(Some(testDatabag))
    Mockito.when(bagRepository.findByName("bar", None)).thenReturn(Some(systemDatabag))
    Mockito.when(bagRepository.findByName("foo", None)).thenReturn(Some(altDatabag))
    Mockito.when(bagRepository.findByTags(Seq("bar"), None)).thenReturn(Seq(systemDatabag))
    Mockito.when(bagRepository.findByTags(Seq("foo"), Some(0))).thenReturn(Seq(testDatabag))
    val createWorkflow = templateService.findTemplate(0, "Projects", "0.1").get.createWorkflow

    def testDatabag : DataBag = {
        val db = DataBag(Some(0), "foo", Seq("foo"), Some(0), Seq(
            DataItem(Some(0), "key1", "fred", None),
            DataItem(Some(0), "key2", "barney", None),
            DataItem(Some(0), "key3", "wilma", None),
            DataItem(Some(0), "key4", "bar", None),
            DataItem(Some(0), "key5", "foo", None)
        ))
        db
    }

    def systemDatabag : DataBag = {
        val db = DataBag(Some(0), "bar", Seq("bar"), None, Seq(
            DataItem(Some(0), "key1", "barney", None),
            DataItem(Some(0), "key2", "wilma", None),
            DataItem(Some(0), "key3", "fred", None)
        ))
        db
    }

    def altDatabag : DataBag = {
        val db = DataBag(Some(0), "foo", Seq("foo"), None, Seq(
            DataItem(Some(0), "key1", "barney", None),
            DataItem(Some(0), "key2", "wilma", None),
            DataItem(Some(0), "key3", "fred", None),
            DataItem(Some(0), "key4", "homer", None),
            DataItem(Some(0), "key5", "zzz", None)
        ))
        db
    }

    @Test def testDefaultValue() {
        val projectVariable: VariableDescription = createWorkflow.variableDescriptions.find(_.name == "projectKey").getOrElse(fail("Variable projectKey must be declared"))
        assert(projectVariable.defaultValue == "barney")
    }

    @Test def testDSConfig() {
        val listVariable: VariableDescription = createWorkflow.variableDescriptions.find(_.name == "projectList").getOrElse(fail("Variable projectList must be declared"))
        expectResult(Some(Map("fred" -> "fred")))(listVariable.values)
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
        val regular: Seq[StepBuilder] = createWorkflow.embody(Map()).regular
        val builder: StepBuilder = regular.head
        val newStep: GenesisStep = builder.newStep
        val actualStep: DoNothingStep = newStep.actualStep.asInstanceOf[DoNothingStep]
        assert(actualStep.name == "wilma")
        val step = regular.tail.head.newStep.actualStep.asInstanceOf[DoNothingStep]
        expectResult("bar")(step.name)
        val lastStep = regular.tail.tail.head.newStep.actualStep.asInstanceOf[DoNothingStep]
        expectResult("zoo")(lastStep.name)
    }
}
