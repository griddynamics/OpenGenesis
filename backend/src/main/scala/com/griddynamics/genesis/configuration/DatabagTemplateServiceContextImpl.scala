package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.beans.factory.annotation.{Value, Autowired}
import com.griddynamics.genesis.repository.DatabagTemplateRepository
import com.griddynamics.genesis.service.DatabagTemplateService
import com.griddynamics.genesis.service.impl.DatabagTemplateServiceImpl
import com.griddynamics.genesis.repository.impl.DatabagTemplateRepositoryImpl

@Configuration
class DatabagTemplateServiceContextImpl extends DatabagTemplateServiceContext {
  @Value("${genesis.databag.template.repository.path:templates}") var templatePath: String = _
  @Value("${genesis.databag.template.repository.wildcard:*.dbtemplate}") var wildCard: String = _

  @Bean
  def databagTemplateRepository: DatabagTemplateRepository = new DatabagTemplateRepositoryImpl(templatePath, wildCard)
  @Bean
  def databagTemplateService: DatabagTemplateService = new DatabagTemplateServiceImpl(databagTemplateRepository)
}
