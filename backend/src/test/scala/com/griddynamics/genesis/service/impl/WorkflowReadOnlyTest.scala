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
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.template.ListVarDSFactory
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import com.griddynamics.genesis.cache.NullCacheManager
import com.griddynamics.genesis.util.{Logging, IoUtil}
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import scala.Some
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api
import com.griddynamics.genesis.plugin.StepBuilder
import org.junit.Test


class WorkflowReadOnlyTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse with Logging {
  val templateService = new GroovyTemplateService(templateRepoService,
    List(new DoNothingStepBuilderFactory), new DefaultConversionService,
    Seq(new ListVarDSFactory, new DependentListVarDSFactory,
      new DatabagDataSourceFactory(databagRepository)), databagRepository, configService, NullCacheManager)

  val bodyWrong = """
  template {
    name("ReadOnlyWrong")
    version("0.1")
    createWorkflow("create")
    destroyWorkflow("destroy")

    workflow("create") {}
    workflow(rdOnly: false) {}
    workflow("destroy") {}
  }"""

  val body = """
  template {
    name("ReadOnly")
    version("0.1")
    createWorkflow("create")
    destroyWorkflow("destroy")

    workflow("create", false) {}
    workflow(name: "test") {}
    workflow(name: "readOnly", readOnly: true) {}
    workflow("destroy") {}
  }"""

  Mockito.when(templateRepository.listSources()).thenReturn(Map(VersionedTemplate("1") -> bodyWrong, VersionedTemplate("2") -> body))
  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  def template = templateService.findTemplate(0, "ReadOnly", "0.1", 0)

  @Test
  def testNoName() {
    expectResult(None)(templateService.findTemplate(0, "ReadOnlyWrong", "0.1", 0))
  }

  @Test
  def testDefaultFalse() {
    expectResult(false)(template.get.getWorkflow("test").get.isReadOnly)
    expectResult(false)(template.get.destroyWorkflow.isReadOnly)
   }

  @Test
  def testReadOnlyTrue() {
    expectResult(true)(template.get.getWorkflow("readOnly").get.isReadOnly)
  }}
