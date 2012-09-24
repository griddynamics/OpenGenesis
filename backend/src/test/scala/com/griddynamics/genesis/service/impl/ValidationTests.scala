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

class ValidationTests extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, CacheManager.getInstance())
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Validations.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    val createWorkflow = templateService.findTemplate(0, "Validations", "0.1").get.createWorkflow

    @Before def setUp() {
        CacheManager.getInstance().clearAll()
    }

    @Test
    def testSimpleValidationWithCustomMessage() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("a" -> "0", "b" -> "0"))
        expect(2)(validate.size)
        expect("Custom error message")(validate.head.description)
        expect("Validation failed")(validate.tail.head.description)
    }

    @Test
    def testSimpleValidationWithCustomMessageAltSyntax() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("foo" -> "5", "a" -> 11, "b" -> 11))
        expect(1)(validate.size)
        expect("Foo message")(validate.head.description)
    }


    @Test
    def testComplexValidation() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("a" -> "10", "b" -> "11", "c" -> "11"))
        expect(1)(validate.size)
        expect("C error message")(validate.head.description)
    }
}
