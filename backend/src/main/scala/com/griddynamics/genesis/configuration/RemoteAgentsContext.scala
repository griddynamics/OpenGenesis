/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Profile, Bean, Configuration}
import akka.actor.ActorSystem
import com.griddynamics.genesis.agents.AgentsHealthServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{ConfigService, AgentsHealthService}
import com.griddynamics.genesis.api.{AgentStatus, RemoteAgent}
import com.griddynamics.genesis.{service, repository}
import com.griddynamics.genesis.repository.impl.RemoteAgentRepositoryImpl
import com.griddynamics.genesis.service.impl.RemoteAgentsServiceImpl

@Configuration
class AgentServiceContext {
  @Bean def agentsRepository: repository.RemoteAgentRepository = new RemoteAgentRepositoryImpl
  @Bean def agentsService(@Autowired healthService: AgentsHealthService): service.RemoteAgentsService = new RemoteAgentsServiceImpl(agentsRepository, healthService)
}

@Configuration
@Profile(Array("server"))
class AgentTrackerContext {

  @Bean def healthService(@Autowired configService: ConfigService, @Autowired actorSystem: ActorSystem): AgentsHealthService = new AgentsHealthServiceImpl(actorSystem, configService)
}

@Configuration
@Profile(Array("genesis-cli"))
class AgentTrackerStubContext {
  @Bean def healthService: AgentsHealthService = new AgentsHealthService {
    def startTracking(agent: RemoteAgent) {}

    def stopTracking(agent: RemoteAgent) {}

    def checkStatus(agents: Seq[RemoteAgent]) = Seq()

    def checkStatus(agent: RemoteAgent) = (AgentStatus.Unavailable, None)
  }

}