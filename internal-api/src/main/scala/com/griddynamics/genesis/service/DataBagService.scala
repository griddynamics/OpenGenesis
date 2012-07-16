package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.DataBag
import com.griddynamics.genesis.common.CRUDService
import org.springframework.transaction.annotation.Transactional

trait DataBagService extends CRUDService[DataBag, Int] {

    @Transactional(readOnly = true)
    def listForProject(projectId: Int): List[DataBag]
}