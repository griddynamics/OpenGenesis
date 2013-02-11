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
import com.griddynamics.genesis.service.ValidationError
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

class ValidationTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse{
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory,
        new DatabagDataSourceFactory(databagRepository)), databagRepository, configService, NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Validations.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

    val createWorkflow = templateService.findTemplate(0, "Validations", "0.1", 1).get.createWorkflow

    @Test
    def testSimpleValidationWithCustomMessage() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("a" -> "0", "b" -> "0"))
        expectResult(2)(validate.size)
        expectResult("Custom error message")(validate.head.description)
        expectResult("Validation failed")(validate.tail.head.description)
    }

    @Test
    def testSimpleValidationWithCustomMessageAltSyntax() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("foo" -> "5", "a" -> 11, "b" -> 11))
        expectResult(1)(validate.size)
        expectResult("Foo message")(validate.head.description)
    }


    @Test
    def testComplexValidation() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("a" -> "10", "b" -> "11", "c" -> "11"))
        expectResult(1)(validate.size)
        expectResult("C error message")(validate.head.description)
    }
}
