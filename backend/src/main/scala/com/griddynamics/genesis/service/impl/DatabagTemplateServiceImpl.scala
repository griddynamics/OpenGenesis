package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.DatabagTemplateService
import com.griddynamics.genesis.repository.DatabagTemplateRepository
import com.griddynamics.genesis.model
import com.griddynamics.genesis.api


class DatabagTemplateServiceImpl(val repository: DatabagTemplateRepository) extends DatabagTemplateService {
  def list = {
    repository.list.map(convert(_))
  }

  def list(scope: String) = repository.list(scope).map(convert(_))

  def get(id: String) = repository.get(id).map(convert(_))

  def scopes = Set(DatabagTemplateRepository.SystemScope, DatabagTemplateRepository.ProjectScope, DatabagTemplateRepository.EnvironmentScope)

  def convert(template: model.DatabagTemplate) : api.DatabagTemplate = {
    new api.DatabagTemplate(template.id, template.name, template.defaultName, template.scope, template.tags.split(",").toSeq,
      template.values.map({case (key,value) =>
        new api.ItemTemplate(key, value.default, value.required)}).toSeq)
  }
}
