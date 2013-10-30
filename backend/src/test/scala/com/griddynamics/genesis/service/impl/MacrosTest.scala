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
import com.griddynamics.genesis.service.{VariableDescription, TemplateDefinition}
import com.griddynamics.genesis.api.{DataItem, DataBag, Failure}
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

  when(templateRepository.listSources()).thenReturn(Map(VersionedTemplate("1") -> body))
  when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

  def testDatabag : DataBag = {
    val db = DataBag(Some(0), "foo", Seq("foo"), Some(0), None, Seq(
      DataItem(Some(0), "key1", "Static", None),
      DataItem(Some(0), "key2", "Dynamic", None)
    ))
    db
  }

  when(databagRepository.findByName("macros", Some(0))).thenReturn(Some(testDatabag))
  when(databagRepository.findByName("macros", None)).thenReturn(Some(testDatabag))

  @Test
  def testStepsInserted() {
    val template: Option[TemplateDefinition] = templateService.findTemplate(0, "Macros", "0.1", 0)
    val workflow = template.flatMap(_.getWorkflow("macros")).get
    val steps = workflow.embody(Map())
    expectResult(8)(steps.regular.size)
    val initialPhase: Option[StepBuilder] = steps.regular.find(_.phase == "auto_0")
    val secondPhase: Option[StepBuilder] = steps.regular.find(_.phase == "auto_1")
    assert(initialPhase.isDefined)
    assert(initialPhase.get.getPrecedingPhases.isEmpty)
    assert(secondPhase.isDefined)
    assert(secondPhase.get.getPrecedingPhases.contains("auto_0"))
    steps.regular.zip(Seq("Static", "Passed from macro call",
        "Set with map", "default", "Set from constant", "Call from closure", "local", "redefine")).map({
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
    val steps = result.embody(Map("myvar" -> "1024"))
    val variables: Seq[VariableDescription] = result.variableDescriptions
    assert(variables.size == 1)
    assert(variables.exists(_.name == "myvar"))
    val step: StepWithMap = steps.regular(0).newStep.actualStep.asInstanceOf[StepWithMap]
    val values: Map[String, String] = step.values
    val text = step.name
    expectResult(Map("operation" -> "Dynamic"))(values)
    expectResult("1024")(text)
    expectResult("bar")(steps.regular(0).getPrecedingPhases.get(0))
  }

  @Test
  def testNullHandling() {
    val template: Option[TemplateDefinition] = templateService.findTemplate(0, "Macros", "0.1", 0)
    val result = template.get.getWorkflow("nulls").get
    val steps = result.embody(Map())
    val step: DoNothingStep = steps.regular(0).newStep.actualStep.asInstanceOf[DoNothingStep]
    val text = step.name
    expectResult(null)(text)
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

