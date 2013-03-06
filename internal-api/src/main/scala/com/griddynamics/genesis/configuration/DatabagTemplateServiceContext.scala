package com.griddynamics.genesis.configuration

import com.griddynamics.genesis.service.DatabagTemplateService


trait DatabagTemplateServiceContext {
  def databagTemplateService: DatabagTemplateService
}
