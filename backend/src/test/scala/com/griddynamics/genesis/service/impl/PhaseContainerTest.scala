package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory}
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


class PhaseContainerTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse with Logging {
  val templateService = new GroovyTemplateService(templateRepoService,
    List(new DoNothingStepBuilderFactory), new DefaultConversionService,
    Seq(new ListVarDSFactory, new DependentListVarDSFactory,
      new DatabagDataSourceFactory(databagRepository)), databagRepository, configService, NullCacheManager)

  val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/PhaseContainer.genesis"))

  Mockito.when(templateRepository.listSources()).thenReturn(Map(VersionedTemplate("1") -> body))
  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  @Test
  def testPhaseApplied() {
    val createWorkflow = templateService.findTemplate(0, "PhaseContainer", "0.1", 0).get.createWorkflow
    val steps = createWorkflow.embody(Map())
    expectResult(4)(steps.regular.size)
    val initialPhase: Option[StepBuilder] = steps.regular.find(_.phase == "initial")
    assert(initialPhase.isDefined)
  }
}
