package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.RemoteAgentsService
import com.griddynamics.genesis.api.{Success, ExtendedResult, RemoteAgent}
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.RemoteAgentRepository


class RemoteAgentsServiceImpl(repository: RemoteAgentRepository) extends RemoteAgentsService {
    def list: Seq[RemoteAgent] = repository.list

    def get(key: Int): Option[RemoteAgent] = repository.get(key)

    def findByTags(tags: Seq[String]): List[RemoteAgent] = repository.findByTags(tags)

    override def update(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
       Success(repository.update(a))
    }

    @Transactional(readOnly = false)
    override def create(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
        Success(repository.insert(a))
    }

    @Transactional(readOnly = false)
    override def delete(a: RemoteAgent): ExtendedResult[RemoteAgent] = {
        a.id.map(repository.delete(_))
        Success(a)
    }
}
