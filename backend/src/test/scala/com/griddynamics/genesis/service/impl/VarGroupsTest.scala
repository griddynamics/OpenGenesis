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

import com.griddynamics.genesis.service.TemplateRepoService
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import org.springframework.core.convert.support.ConversionServiceFactory
import org.junit.{Test, Before}
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.repository.DatabagRepository
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.service.ValidationError

class VarGroupsTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, CacheManager.getInstance())
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/VarGroups.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    val createWorkflow = templateService.findTemplate(0, "VariableGroups", "0.1").get.createWorkflow

    @Before def setUp() {
        CacheManager.getInstance().clearAll()
    }

    @Test
    def testSimpleGroup() {
      expect(None)(createWorkflow.variableDescriptions.find(_.name == "a").get.group)
      expect(Some("testGroup"))(createWorkflow.variableDescriptions.find(_.name == "b").get.group)
      expect(Some("testGroup"))(createWorkflow.variableDescriptions.find(_.name == "c").get.group)
    }

    @Test
    def testValidateSuccess() {
      expect(Seq())(createWorkflow.validate(Map("a" -> 1, "b" -> true, "y" -> 3, "x" -> "s")))
    }

    @Test
    def testValidateFail() {
      val errors = createWorkflow.validate(Map("a" -> 1, "b" -> false, "c" -> 3, "y" -> 0))
      expect(2)(errors.size)
      expect("b")(errors.head.variableName)
      expect("No more than one variable in group 'testGroup' could have value")(errors.head.description)
      expect("c")(errors.tail.head.variableName)
     }

    @Test
    def testValidateRequiredFail() {
      val errors = createWorkflow.validate(Map("a" -> 1))
      expect(1)(errors.size)
    }
}
