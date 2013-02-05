/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.api.Configuration
import com.griddynamics.genesis.cache.NullCacheManager
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.service.{EnvironmentService, WorkflowDefinition, TemplateRepoService}
import com.griddynamics.genesis.template.{VersionedTemplate, TemplateRepository}
import org.junit.runner.RunWith
import org.mockito.{Matchers, Mockito}
import org.scalatest.FunSpec
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.core.convert.support.DefaultConversionService

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class Groovy$envConfigTest extends AssertionsForJUnit with MockitoSugar with ShouldMatchers with FunSpec {
  val body =
    """
      template {
          name("envConfigTest")
          version("0.1")
          createWorkflow("create")
          destroyWorkflow("destroy")

          workflow("create") {
              variables {
                variable("test").validator (
                  "Requires 'test_settings' in env config" : {
                     $envConfig["test_settings"] as Boolean
                  }
                )
              }
              steps { }
          }

          workflow("destroy") { steps { } }
      }
    """
  val templateRepository = mock[TemplateRepository]
  val templateRepoService = mock[TemplateRepoService]
  val envConfigService = mock[EnvironmentService]

  Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)

  val defaultConfig = new Configuration(Some(1), "default", 1, None, items = Map(), instanceCount = Some(1))

  Mockito.when(envConfigService.getDefault(Matchers.anyInt)).thenReturn(Option(defaultConfig))
  Mockito.when(envConfigService.list(Matchers.anyInt)).thenReturn(Seq(defaultConfig))

  val templateService = new GroovyTemplateService(templateRepoService, List(), new DefaultConversionService,
    Seq(), mock[DatabagRepository], envConfigService, NullCacheManager)
  Mockito.when(templateRepository.listSources()).thenReturn(Map(VersionedTemplate("1") -> body))


  describe("$envConfig access from variable validation") {
    it("should find no validation errors if env config has required property") {
      val template = templateService.findTemplate(0, "envConfigTest", "0.1").get
      val workflow = template.getWorkflow("create").get

      val errors = workflow.validate(Map("test" -> "aaa"), Some(defaultConfig.copy(items = Map("test_settings" -> "true"))))

      errors should have size 0
    }

    it("should find 1 validation error if env config hasn't got required properties") {
      val template = templateService.findTemplate(0, "envConfigTest", "0.1").get
      val workflow: WorkflowDefinition = template.getWorkflow("create").get

      val errors = workflow.validate(Map("test" -> "aaa"), Some(defaultConfig.copy(items = Map())))

      errors should have size 1
      errors.head.variableName should be  === "test"
    }

    it("should throw no exceptions if Config context is not provided"){
      val template = templateService.findTemplate(0, "envConfigTest", "0.1").get
      val workflow = template.getWorkflow("create").get

      workflow.validate(Map("test" -> "aaa"), config = None)
    }
  }

}
