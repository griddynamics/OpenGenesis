package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.DataBag
import com.griddynamics.genesis.common.CRUDService

trait DataBagService extends CRUDService[DataBag, Int] {

    def listForProject(projectId: Int): List[DataBag]
}