package com.griddynamics.genesis.agent

import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.agents.configuration.{ConfigurationApplied, ConfigurationResponse}

@Configuration
class ConfigContext {
    @Bean def configService: SimpleConfigService = new AgentConfigService
}

trait SimpleConfigService {
  def getConfig: ConfigurationResponse
  def applyConfiguration(request: Map[String,String]) : ConfigurationApplied
}

class AgentConfigService extends SimpleConfigService {
  def getConfig: ConfigurationResponse = new ConfigurationResponse
  def applyConfiguration(request: Map[String,String]) = ConfigurationApplied(restart = false)
}
