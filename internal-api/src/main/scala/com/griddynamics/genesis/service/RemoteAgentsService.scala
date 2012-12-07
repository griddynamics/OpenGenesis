package com.griddynamics.genesis.service

import com.griddynamics.genesis.common.CRUDService
import com.griddynamics.genesis.api.RemoteAgent
import org.springframework.transaction.annotation.Transactional

trait RemoteAgentsService extends CRUDService[RemoteAgent, Int]{
    @Transactional(readOnly = true)
    def findByTags(tags: Seq[String]): List[RemoteAgent]
}
