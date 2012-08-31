package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.template.{RequirementsNotMetException, VersionedTemplate, ListVarDSFactory, TemplateRepository}
import org.springframework.core.convert.support.ConversionServiceFactory
import org.junit.{Test, Before}
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.repository.DatabagRepository
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.service.{WorkflowDefinition, TemplateDefinition, ValidationError}
import com.griddynamics.genesis.api.{ExtendedResult, Failure}

class PreconditionsTests extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateService = new GroovyTemplateService(templateRepository,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, CacheManager.getInstance())
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Preconditions.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(bagRepository.findByName("foo")).thenReturn(None)

    @Before def setUp() {
        CacheManager.getInstance().clearAll()
    }

    @Test
    def testPreconditions() {
        val definition: TemplateDefinition = templateService.findTemplate(0, "Precondidions", "0.1").get
        assert(definition != null)
        val workflow: ExtendedResult[WorkflowDefinition] = definition.getValidWorkflow(definition.createWorkflow.name)
        expect(Failure(compoundServiceErrors = Seq("Second requirement not met")))(workflow)
    }

}
