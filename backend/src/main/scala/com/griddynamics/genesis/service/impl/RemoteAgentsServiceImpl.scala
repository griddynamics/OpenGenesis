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

import com.griddynamics.genesis.service.{AgentsHealthService, RemoteAgentsService}
import com.griddynamics.genesis.api.{Success, ExtendedResult, RemoteAgent}
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.RemoteAgentRepository


class RemoteAgentsServiceImpl(repository: RemoteAgentRepository, val health: AgentsHealthService) extends RemoteAgentsService {

    repository.list.map { health.startTracking(_) }

    private def withStatus(agents: Seq[RemoteAgent]): Seq[RemoteAgent] = {
      health.checkStatus(agents).map { case (agent, (status, jobs)) => agent.copy(status = Some(status), stats = jobs) }
    }

    def list: Seq[RemoteAgent] = withStatus(repository.list)

    def get(key: Int): Option[RemoteAgent] = repository.get(key).map(
      agent => {
        val s = health.checkStatus(agent)
        agent.copy(status = Some(s._1), stats = s._2)
      }
    )

    def findByTags(tags: Seq[String]): Seq[RemoteAgent] = withStatus(repository.findByTags(tags))

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
