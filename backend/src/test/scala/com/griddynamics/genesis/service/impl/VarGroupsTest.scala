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

import com.griddynamics.genesis.template.ListVarDSFactory
import org.springframework.core.convert.support.DefaultConversionService
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.{Matchers, Mockito}
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.cache.NullCacheManager
import org.mockito.Mockito._
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

class VarGroupsTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse {
    Mockito.when(configService.getDefault(Matchers.anyInt)).thenReturn(None)
    Mockito.when(configService.list(Matchers.anyInt)).thenReturn(Seq())
    when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(databagRepository)),
      databagRepository, configService, NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/VarGroups.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    val createWorkflow = templateService.findTemplate(0, "VariableGroups", "0.1", 0).get.createWorkflow

    @Test
    def testSimpleGroup() {
      expectResult(None)(createWorkflow.variableDescriptions.find(_.name == "a").get.group)
      expectResult(Some("testGroup"))(createWorkflow.variableDescriptions.find(_.name == "b").get.group)
      expectResult(Some("testGroup"))(createWorkflow.variableDescriptions.find(_.name == "c").get.group)
    }

    @Test
    def testValidateSuccess() {
      expectResult(Seq())(createWorkflow.validate(Map("a" -> 1, "b" -> true, "y" -> 3, "x" -> "s")))
    }

    @Test
    def testValidateFail() {
      val errors = createWorkflow.validate(Map("a" -> 1, "b" -> false, "c" -> 3, "y" -> 0))
      expectResult(2)(errors.size)
      expectResult("b")(errors.head.variableName)
      expectResult("No more than one variable in group 'testGroup' could have value")(errors.head.description)
      expectResult("c")(errors.tail.head.variableName)
     }

    @Test
    def testValidateRequiredFail() {
      val errors = createWorkflow.validate(Map("a" -> 1))
      expectResult(1)(errors.size)
    }
}
