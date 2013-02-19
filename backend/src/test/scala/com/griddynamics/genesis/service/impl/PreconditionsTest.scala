package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.template._
import org.springframework.core.convert.support.DefaultConversionService
import org.junit.Test
import com.griddynamics.genesis.util.IoUtil
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import org.scalatest.mock.MockitoSugar
import org.mockito.{Matchers, Mockito}
import org.scalatest.junit.AssertionsForJUnit
import com.griddynamics.genesis.service.{WorkflowDefinition, TemplateDefinition}
import com.griddynamics.genesis.api.ExtendedResult
import com.griddynamics.genesis.cache.NullCacheManager
import org.mockito.Mockito._
import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

class PreconditionsTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse {

    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(databagRepository)),
      databagRepository, configService, NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Preconditions.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    Mockito.when(databagRepository.findByName("foo")).thenReturn(None)
    when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

    @Test
    def testPreconditions() {
        val definition: TemplateDefinition = templateService.findTemplate(0, "Precondidions", "0.1", 1).get
        assert(definition != null)
        val workflow: ExtendedResult[WorkflowDefinition] = definition.getValidWorkflow(definition.createWorkflow.name)
        expectResult(Failure(compoundServiceErrors = Seq("Second requirement not met")))(workflow)
    }

}
