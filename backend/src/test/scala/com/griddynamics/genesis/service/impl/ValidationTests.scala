package com.griddynamics.genesis.service.impl

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
    val templateService = new GroovyTemplateService(templateRepository,
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
        assert(validate.size == 2)
        assert(validate.head.description == "Custom error message")
        assert(validate.tail.head.description == "Validation failed")
    }

    @Test
    def testSimpleValidationWithCustomMessageAltSyntax() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("foo" -> "5", "a" -> 11, "b" -> 11))
        assert(validate.size == 1)
        assert(validate.head.description == "Foo message")
    }


    @Test
    def testComplexValidation() {
        val validate: Seq[ValidationError] = createWorkflow.validate(Map("a" -> "10", "b" -> "11", "c" -> "11"))
        assert(validate.size == 1)
        assert(validate.head.description == "C error message")
    }

}
