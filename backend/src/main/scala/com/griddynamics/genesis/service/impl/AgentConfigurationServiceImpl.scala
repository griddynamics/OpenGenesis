package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.service.AgentConfigurationService
import com.griddynamics.genesis.api.{Failure, Success, RemoteAgent}
import akka.actor.{RootActorPath, ActorContext, ActorRef, ActorSystem}
import akka.pattern.ask
import com.griddynamics.genesis.agents.AgentGateway
import akka.util.Timeout
import scala.concurrent.duration._
import com.griddynamics.genesis.agents.configuration.{ConfigurationApplied, ApplyConfiguration, ConfigurationResponse, GetConfiguration}
import scala.concurrent.Await


class AgentConfigurationServiceImpl(actorSystem: ActorSystem) extends AgentConfigurationService with Logging {
  implicit val requestTimeout = Timeout(5 seconds)

  def getConfiguration(agent: RemoteAgent) = try {
    val agentActor = createActorForAgent(agent)
    val future = (agentActor ? GetConfiguration).mapTo [ConfigurationResponse]
    val result: ConfigurationResponse = Await.result(future, requestTimeout.duration)
    Success(result.names)
  } catch {
    case e: Exception => {
      log.error(s"Error getting configuration for agent $agent", e)
      Failure(compoundServiceErrors = Seq(s"Error getting configuration for agent $agent", e.getMessage))
    }
  }

  def applyConfiguration(agent: RemoteAgent, values: Map[String, String]) = try {
    val agentActor = createActorForAgent(agent)
    val future = (agentActor ? ApplyConfiguration(values)).mapTo [ConfigurationApplied]
    val result = Await.result(future, requestTimeout.duration)
    if (result.success)
       Success(result.restart)
    else
       Failure(compoundServiceErrors = Seq("Failed to apply configuration"))
  } catch {
    case e: Exception => {
      log.error(s"Failed to apply configuration to agent $agent", e)
      Failure(compoundServiceErrors = Seq("Failed to apply configuration", e.getMessage))
    }
  }

  private def createActorForAgent(agent: RemoteAgent) : ActorRef = {
    actorSystem.actorFor(RootActorPath(AgentGateway.address(agent)) / "user" / "frontActor")
  }


}
