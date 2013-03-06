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
import org.mockito.{Matchers, Mockito}
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.template.dsl.groovy.WorkflowDeclaration
import scala.Array
import com.griddynamics.genesis.service.{EnvironmentConfigurationService, TemplateRepoService}
import com.griddynamics.genesis.template.TemplateRepository
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.cache.NullCacheManager
import org.mockito.Mockito._
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

class GroovyTemplateSyntaxTest extends AssertionsForJUnit with Logging with MockitoSugar {

    //Template declaration
    
    @Test def testEvaluationEmptyScript() {
        assert(getTemplateDefinition("").isEmpty)
    }

    def testEvaluationUndefinedMethod() {
        assert(getTemplateDefinition("workflow(\"create\")").isEmpty)
    }

    @Test def testEvaluationSimpleScript() {
        assert(getTemplateDefinition("template()").isEmpty)
    }

    @Test def testEvaluationScriptAllMandatoryFields() {
        val templateDefinition = getTemplateDefinition(
            "template{\n" +
                joinExclude(mandatoryMethodsOfTemplate, Array()) +
                "}").get
        assert(templateDefinition.listWorkflows.size === 2)
        assert(templateDefinition.createWorkflow.name === "createName")
        assert(templateDefinition.createWorkflow.variableDescriptions.size === 0)
        assert(templateDefinition.createWorkflow.embody(Map()).regular.size === 0)
        assert(templateDefinition.createWorkflow.validate(Map()).size === 0)
        assert(templateDefinition.destroyWorkflow.name === "destroyName")
        assert(templateDefinition.destroyWorkflow.variableDescriptions.size === 0)
        assert(templateDefinition.destroyWorkflow.embody(Map()).regular.size === 0)
        assert(templateDefinition.destroyWorkflow.validate(Map()).size === 0)
        assert(templateDefinition.getWorkflow("createName").get.name === templateDefinition.createWorkflow.name)
        assert(templateDefinition.getWorkflow("destroyName").get.name === templateDefinition.destroyWorkflow.name)
    }

    @Test(expected = classOf[java.lang.Exception])
    def testEvaluationScriptMandatoryOmitted() {
        mandatoryMethodsOfTemplate.foreach(string => {
            getTemplateDefinition(
                "template{\n" +
                    joinExclude(mandatoryMethodsOfTemplate, Array(string)) +
                    "}").get
        })
    }

    @Test(expected = classOf[java.lang.Exception])
    def testEvaluationScriptMandatoryDuplicated() {
        mandatoryMethodsOfTemplate.foreach(string => {
            getTemplateDefinition(
                "template{\n" +
                    joinDuplicate(mandatoryMethodsOfTemplate, Array(string)) +
                    "}").get
        })
    }

    @Test def testEvaluationScriptManyWorkflows() {
        var workflows: String = ""
        for (i <- 0 until 1000) {
            workflows += "workflow(\"Workflow " + i.toString + "\") {}\n"
        }


        val templateDefinition = getTemplateDefinition(
            "template{\n" +
                workflows +
                joinExclude(mandatoryMethodsOfTemplate, Array()) +
                "}").get

        assert(templateDefinition.listWorkflows.size === 1002)
        assert(templateDefinition.createWorkflow.name === "createName")
        assert(templateDefinition.createWorkflow.variableDescriptions.size === 0)
        assert(templateDefinition.createWorkflow.embody(Map()).regular.size === 0)
        assert(templateDefinition.createWorkflow.validate(Map()).size === 0)
        assert(templateDefinition.destroyWorkflow.name === "destroyName")
        assert(templateDefinition.destroyWorkflow.variableDescriptions.size === 0)
        assert(templateDefinition.destroyWorkflow.embody(Map()).regular.size === 0)
        assert(templateDefinition.destroyWorkflow.validate(Map()).size === 0)
        assert(templateDefinition.getWorkflow("createName").get.name === templateDefinition.createWorkflow.name)
        assert(templateDefinition.getWorkflow("destroyName").get.name === templateDefinition.destroyWorkflow.name)
    }

    //Workflow declaration

    @Test def testOverrideVariables() {
        classOf[WorkflowDeclaration].getDeclaredMethods
            .filter(
            method => method.getGenericReturnType.equals(Void.TYPE))
            //            .filter(method=>method.getGenericParameterTypes.equals(Array(classOf[Closure[BoxedUnit]])))
            .foreach(
            method => {
                var a = method
                println(method)
            })

    }

  @Test def testEvaluationOneVariable() {
    val script = """template {
          name("erlang")
          version("0.1")
          createWorkflow("create")
          destroyWorkflow("destroy")

          workflow("create") {
              variables {
                  variable("nodesCount").as(Integer)
                      .description("Erlang worker nodes count")
                      .validator { it > 0 }

                  variable("test").description("test")
              }
              steps {
                  teststep {
                      phase = "provision"
                      text = nodesCount
                  }
                  teststep {
                      phase = "install"
                      precedingPhases = ["provision"]
                      text = "erlang2"
                  }
              }
          }
          workflow("destroy") {
              steps {
                  teststep {
                      phase = "undeploy"
                      text = "destroy"
                  }
              }
          }
      }"""
    val template = getTemplateDefinition(script)
    assert(template.isDefined)
    val nodesCountVar = template.get.createWorkflow.variableDescriptions.find(_.name == "nodesCount")
    assert(nodesCountVar.isDefined)
    expectResult(None)(nodesCountVar.get.values)
  }


    def getTemplateDefinition(script: String) = {
        val templateRepository = mock[TemplateRepository]
        val templateRepoService = mock[TemplateRepoService]
        val bagRepository = mock[DatabagRepository]
        Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> script))
        Mockito.when(templateRepository.getContent(VersionedTemplate("1") )).thenReturn(Some(script))
        Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)

        val configService  = mock[EnvironmentConfigurationService]
        when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

        val templateService = new GroovyTemplateService(templateRepoService,
            List(
              new DoNothingStepBuilderFactory
            ),
           new DefaultConversionService, Seq(), bagRepository, configService, NullCacheManager)

        templateService.listTemplates(0).headOption match {
            case Some(t) => templateService.findTemplate(0, t.name, t.version, 0)
            case None => None
        }
    }

    val mandatoryMethodsOfTemplate = Array[String](
        "name(\"erlang\")\n",
        "version(\"0.1\")\n",
        "createWorkflow(\"createName\")\n",
        "destroyWorkflow(\"destroyName\")\n",
        "workflow(\"createName\") {}\n",
        "workflow(\"destroyName\") {}\n"
    )

    def joinExclude(strings: Array[String], excludes: Array[String]) = {
        var joinedString: String = ""
        strings.foreach(string => {
            if (!excludes.contains(string))
                joinedString += string
            else
                log.debug("Skipped:" + string)
        })
        joinedString
    }

    def joinDuplicate(strings: Array[String], duplicates: Array[String]) = {
        var joinedString: String = ""
        strings.foreach(string => {
            if (!duplicates.contains(string)) {
                joinedString += string
                log.debug("Skipped:" + string)
            }
            joinedString += string
        })
        joinedString
    }


}