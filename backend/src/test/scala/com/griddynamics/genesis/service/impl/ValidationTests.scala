package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.{EnvironmentService, TemplateRepoService, ValidationError}
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import org.springframework.core.convert.support.DefaultConversionService
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.repository.{ConfigurationRepository, DatabagRepository}
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.{Matchers, Mockito}
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.cache.NullCacheManager

class ValidationTests extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory,
        new DatabagDataSourceFactory(bagRepository)), bagRepository, mock[EnvironmentService], NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Validations.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    val createWorkflow = templateService.findTemplate(0, "Validations", "0.1").get.createWorkflow

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
