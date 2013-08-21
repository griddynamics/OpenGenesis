package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.template.ListVarDSFactory
import com.griddynamics.genesis.util.{Logging, IoUtil}
import org.mockito.{Matchers, Mockito}
import org.mockito.Mockito._
import scala.Some
import org.junit.Test
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import com.griddynamics.genesis.cache.NullCacheManager
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api
import com.griddynamics.genesis.plugin.{StepBuilderFactory, StepBuilder}
import com.griddynamics.genesis.service.TemplateDefinition
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.workflow.Step
import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.JavaConversions._


class MacrosTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse with Logging {
  val templateService = new GroovyTemplateService(templateRepoService,
    List(new DoNothingStepBuilderFactory, new StepWithMapFactory), new DefaultConversionService,
    Seq(new ListVarDSFactory, new DependentListVarDSFactory,
      new DatabagDataSourceFactory(databagRepository)), databagRepository, configService, NullCacheManager)

  val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Macros.genesis"))

  Mockito.when(templateRepository.listSources()).thenReturn(Map(VersionedTemplate("1") -> body))
  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  @Test
  def testStepsInserted() {
    val template: Option[TemplateDefinition] = templateService.findTemplate(0, "Macros", "0.1", 0)
    val workflow = template.flatMap(_.getWorkflow("macros")).get
    val steps = workflow.embody(Map())
    expectResult(5)(steps.regular.size)
    val initialPhase: Option[StepBuilder] = steps.regular.find(_.phase == "auto_0")
    val secondPhase: Option[StepBuilder] = steps.regular.find(_.phase == "auto_1")
    assert(initialPhase.isDefined)
    assert(initialPhase.get.getPrecedingPhases.isEmpty)
    assert(secondPhase.isDefined)
    assert(secondPhase.get.getPrecedingPhases.contains("auto_0"))
    steps.regular.zip(Seq("Static", "Passed from macro call",
        "Set with map", "default", "Set from constant")).map({
      case (step, message) => step.newStep.actualStep match {
        case nothing: DoNothingStep => expectResult(message)(nothing.name)
      }
    })
  }

  @Test
  def testFullRequireFromMacro() {
    val template: Option[TemplateDefinition] = templateService.findTemplate(0, "Macros", "0.1", 0)
    assert(template.isDefined)
    val result = template.map(_.getValidWorkflow("macros")).get
    expectResult(Failure(compoundServiceErrors = Seq("Oops", "Oops again")))(result)
  }

  @Test
  def testMacroWithMap() {
    val template: Option[TemplateDefinition] = templateService.findTemplate(0, "Macros", "0.1", 0)
    val result = template.get.getWorkflow("maps").get
    val steps = result.embody(Map())
    val values: Map[String, String] = steps.regular(0).newStep.actualStep.asInstanceOf[StepWithMap].values
    expectResult(Map("operation" -> "subst"))(values)
  }
}

case class StepWithMap(name: String, values: Map[String,String]) extends Step {
  override def stepDescription = "Best step ever!"
}

class StepWithMapFactory extends StepBuilderFactory {
  val stepName = "withMap"

  def newStepBuilder = new StepBuilder {
    @BeanProperty var text: String = _
    @BeanProperty var map: java.util.Map[String, String] = _

    def getDetails = StepWithMap(text, map.toMap)
  }
}

