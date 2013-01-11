package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.service.TemplateRepoService
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import com.griddynamics.genesis.repository.DatabagRepository
import org.springframework.core.convert.support.{DefaultConversionService, ConversionServiceFactory}
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import com.griddynamics.genesis.util.IoUtil
import org.junit.{Test, Before}
import org.mockito.Mockito
import com.griddynamics.genesis.cache.NullCacheManager

class RescueTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateRepoService = mock[TemplateRepoService]
    Mockito.when(templateRepoService.get(0)).thenReturn(templateRepository)
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, NullCacheManager)
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Rescue.genesis"))
    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))

    @Test
    def testRescue() {
        val createWorkflow = templateService.findTemplate(0, "Rescue", "0.1").get.createWorkflow
        val steps = createWorkflow.embody(Map())
        expect(1)(steps.onError.size)
    }
}
