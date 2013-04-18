package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.{ExtendedResult, RemoteAgent}

trait AgentConfigurationService {
  def getConfiguration(agent: RemoteAgent) : ExtendedResult[Seq[String]]
  def applyConfiguration(agent: RemoteAgent, values: Map[String, String]): ExtendedResult[Boolean]
}
