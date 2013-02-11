package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.template.ListVarDSFactory
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import com.griddynamics.genesis.util.IoUtil
import org.junit.Test
import org.mockito.{Matchers, Mockito}
import com.griddynamics.genesis.cache.NullCacheManager
import org.mockito.Mockito._
import com.griddynamics.genesis.template.VersionedTemplate
import com.griddynamics.genesis.api

class RescueTest extends AssertionsForJUnit with MockitoSugar with DSLTestUniverse {
    val templateService = new GroovyTemplateService(templateRepoService,
        List(new DoNothingStepBuilderFactory), new DefaultConversionService,
        Seq(new ListVarDSFactory, new DependentListVarDSFactory,
        new DatabagDataSourceFactory(databagRepository)), databagRepository, configService, NullCacheManager)

    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Rescue.genesis"))

    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))
    when(configService.get(Matchers.any(), Matchers.any())).thenReturn(Some(new api.Configuration(Some(0), "", 0, None, Map())))

    @Test
    def testRescue() {
        val createWorkflow = templateService.findTemplate(0, "Rescue", "0.1", 0).get.createWorkflow
        val steps = createWorkflow.embody(Map())
        expectResult(1)(steps.onError.size)
    }
}
