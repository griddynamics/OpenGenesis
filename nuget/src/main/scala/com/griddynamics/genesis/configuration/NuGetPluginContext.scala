package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.nuget.datasource.NuGetDataSourceFactory

@Configuration
class NuGetPluginContext {
  @Autowired var credStore: CredentialsStoreService = _
  @Bean
  def nugetDataSourceFactory = {
    new NuGetDataSourceFactory(credStore)
  }
}
