/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.{AgentConfigurationService, AgentsHealthService, RemoteAgentsService}
import com.griddynamics.genesis.api._
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.RemoteAgentRepository
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.api.RemoteAgent
import com.griddynamics.genesis.api.Failure
import scala.Some
import com.typesafe.config.{ConfigObject, ConfigFactory}
import com.griddynamics.genesis.configuration.GenesisSettingMetadata
import collection.JavaConversions.mapAsScalaMap
import com.griddynamics.genesis.validation.{ConfigValueValidator, SettingsValidation}
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout
import java.util.concurrent.TimeoutException


class RemoteAgentsServiceImpl(repository: RemoteAgentRepository, val health: AgentsHealthService,
                              val config: AgentConfigurationService,
                              override val validators: Map[String, ConfigValueValidator],
                              override val defaultValidator: ConfigValueValidator) extends RemoteAgentsService with SettingsValidation {

    override val defaults: Map[String, GenesisSettingMetadata] = ConfigFactory.load("genesis-plugin").
      root.toMap.filterKeys(_.startsWith("genesis.")).mapValues {
      case co: ConfigObject => new GenesisSettingMetadata(co.toConfig)
    }

    repository.list.map { health.startTracking(_) }


    def status(agent: RemoteAgent) = health.checkStatus(agent)
    def status(agents: Seq[RemoteAgent]) = health.checkStatus(agents)

    @Transactional(readOnly = true)
    def list: Seq[RemoteAgent] = repository.list

  @Transactional(readOnly = true)
     def get(key: Int): Option[RemoteAgent] = repository.get(key).map(
      agent => {
        val future = health.checkStatus(agent)
        val s = try {
          Await.result(future, Timeout(2 seconds).duration)
        } catch {
          case e: TimeoutException => (AgentStatus.Unavailable, None)
        }
        agent.copy(status = Some(s._1), stats = s._2)
      }
    )

  private def getDefault(key: String, value: String) = {
    if (value == null || value.isEmpty) {
      defaults.get(key).map(metadata => metadata.default).getOrElse("")
    } else value
  }

  def getConfiguration(key: Int) = repository.get(key).map(
    agent => {
      config.getConfiguration(agent).map(response => {
        response.map({case (k,v) => {
          val default = defaults.getOrElse(k, GenesisSettingMetadata("NOT-SET!!!"))
          new ConfigProperty(k, v, false, default.description, default.propType, false)
        }}).toSeq
      })
    }
  ).getOrElse(Failure(isNotFound = true))

  def putConfiguration(values: Map[String, String], key: Int) = get(key).map(
    {
      case agent if (agent.status.isDefined && agent.status.get == AgentStatus.Active) => {
        val validationResult: ExtendedResult[Any] = values.map({
          case (name, value) => validate(name, value)
        }).reduceOption(_ ++ _).getOrElse(Success())
        validationResult.flatMap( r =>
          config.applyConfiguration(agent,values).flatMap(_ => Success(agent))
        )
      }
    }
  ).getOrElse(Failure(isNotFound = true))

    @Transactional(readOnly = true)
    def findByTags(tags: Seq[String]): Seq[RemoteAgent] = repository.findByTags(tags).toList //to detach from result set

    @Transactional(readOnly = false)
    override def update(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
      repository.get(a.id.get).map(health.stopTracking(_))
      val updated = repository.update(a)
      health.startTracking(a)
      Success(updated)
    }

    @Transactional(readOnly = false)
    override def create(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
      val agent: RemoteAgent = repository.insert(a)
      health.startTracking(agent)
      Success(agent)
    }

    @Transactional(readOnly = false)
    override def delete(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
      a.id.map(repository.delete(_))
      health.stopTracking(a)
      Success(a)
    }
}
