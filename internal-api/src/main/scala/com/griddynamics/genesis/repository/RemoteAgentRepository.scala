package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api.RemoteAgent
import org.springframework.transaction.annotation.Transactional

trait RemoteAgentRepository extends Repository[RemoteAgent]{
    @Transactional(readOnly = true)
    def findByTags(tags: Seq[String]) : Seq[RemoteAgent]

    @Transactional(readOnly = false)
    def touch(key: Int)
}
