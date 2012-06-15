package com.griddynamics.genesis.model

import java.sql.Timestamp
import com.griddynamics.genesis.workflow.Action


class ActionTracking(val workflowStepId: GenesisEntity.Id, val actionName: String, val actionUUID: String, val desc: Option[String], val started: Timestamp,
                     val finished: Option[Timestamp] = None) extends GenesisEntity

object ActionTracking {
    def apply(stepId: Int, action: Action) = new ActionTracking(stepId,
        action.desc, action.uuid, None, new Timestamp(System.currentTimeMillis()))
}