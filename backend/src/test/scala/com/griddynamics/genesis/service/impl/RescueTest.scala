package com.griddynamics.genesis.service.impl

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar
import com.griddynamics.genesis.template.{VersionedTemplate, ListVarDSFactory, TemplateRepository}
import com.griddynamics.genesis.repository.DatabagRepository
import org.springframework.core.convert.support.ConversionServiceFactory
import com.griddynamics.genesis.template.support.DatabagDataSourceFactory
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.util.IoUtil
import org.junit.{Test, Before}
import com.griddynamics.genesis.plugin.StepBuilder
import org.mockito.Mockito

class RescueTest extends AssertionsForJUnit with MockitoSugar {
    val templateRepository = mock[TemplateRepository]
    val bagRepository = mock[DatabagRepository]
    val templateService = new GroovyTemplateService(templateRepository,
        List(new DoNothingStepBuilderFactory), ConversionServiceFactory.createDefaultConversionService(),
        Seq(new ListVarDSFactory, new DependentListVarDSFactory, new DatabagDataSourceFactory(bagRepository)), bagRepository, CacheManager.getInstance())
    val body = IoUtil.streamAsString(classOf[GroovyTemplateServiceTest].getResourceAsStream("/groovy/Rescue.genesis"))
    Mockito.when(templateRepository.listSources).thenReturn(Map(VersionedTemplate("1") -> body))

    @Before def setUp() {
        CacheManager.getInstance().clearAll()
    }

    @Test
    def testRescue() {
        val createWorkflow = templateService.findTemplate(0, "Rescue", "0.1").get.createWorkflow
        val steps = createWorkflow.embody(Map())
        expect(1)(steps.onError.size)
    }
}
