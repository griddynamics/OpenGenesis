package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.template._
import org.springframework.core.convert.support.DefaultConversionService
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.repository.{DatabagRepository}
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.service.{EnvironmentService, TemplateRepoService, WorkflowDefinition, TemplateDefinition}
import com.griddynamics.genesis.api.ExtendedResult
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.cache.NullCacheManager

class PreconditionsTests extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
    val bagRepository = mock[DatabagRepository]
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)),
      bagRepository, mock[EnvironmentService], NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Preconditions.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(bagRepository.findByName("foo")).thenReturn(None)

    @Test
    def testPreconditions() {
        val definition: TemplateDefinition = templateService.findTemplate(0, "Precondidions", "0.1").get
        assert(definition != null)
        val workflow: ExtendedResult[WorkflowDefinition] = definition.getValidWorkflow(definition.createWorkflow.name)
        expectResult(Failure(compoundServiceErrors = Seq("Second requirement not met")))(workflow)
    }

}
