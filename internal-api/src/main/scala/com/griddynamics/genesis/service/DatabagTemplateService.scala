package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.DatabagTemplate

trait DatabagTemplateService {
  def list : List[DatabagTemplate]
  def list(scope: String) : List[DatabagTemplate]
  def get(id: String) : Option[DatabagTemplate]
  def scopes: Set[String]
}
