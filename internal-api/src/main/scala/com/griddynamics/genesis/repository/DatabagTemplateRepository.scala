package com.griddynamics.genesis.repository

import com.griddynamics.genesis.model.DatabagTemplate

trait DatabagTemplateRepository {
   def list: List[DatabagTemplate]
   def list(scope: String): List[DatabagTemplate] = list.filter(scope == _.scope)
   def get(id: String) = list.find(_.id == id)
}

object DatabagTemplateRepository {
  val ProjectScope = "project"
  val SystemScope = "system"
  val EnvironmentScope = "environment"
}
