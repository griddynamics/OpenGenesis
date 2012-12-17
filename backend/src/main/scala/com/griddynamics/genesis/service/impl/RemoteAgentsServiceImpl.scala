package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.{AgentsHealthService, RemoteAgentsService}
import com.griddynamics.genesis.api.{Success, ExtendedResult, RemoteAgent}
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.RemoteAgentRepository


class RemoteAgentsServiceImpl(repository: RemoteAgentRepository, health: AgentsHealthService) extends RemoteAgentsService {

    repository.list.map { health.startTracking(_) }

    private def withStatus(agents: Seq[RemoteAgent]): Seq[RemoteAgent] = {
      health.checkStatus(agents).map { case (agent, status) => agent.copy(status = Some(status)) }
    }

    def list: Seq[RemoteAgent] = withStatus(repository.list)

    def get(key: Int): Option[RemoteAgent] = repository.get(key).map(
      agent => agent.copy(status = Option(health.checkStatus(agent)))
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
