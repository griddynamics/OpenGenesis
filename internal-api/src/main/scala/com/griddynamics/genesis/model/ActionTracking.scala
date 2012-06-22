package com.griddynamics.genesis.model

import java.sql.Timestamp
import com.griddynamics.genesis.workflow.Action


class ActionTracking(val workflowStepId: GenesisEntity.Id, val actionName: String, val actionUUID: String, val description: Option[String],
                     val started: Timestamp, val finished: Option[Timestamp] = None, val status: ActionTrackingStatus.ActionStatus) extends GenesisEntity {
    def this() = this(0, "", "", None, new Timestamp(System.currentTimeMillis()), None, ActionTrackingStatus.Executing)
}
object ActionTrackingStatus extends Enumeration {
    type ActionStatus = Value
    val Executing = Value(0, "Executing")
    val Failed = Value(1, "Failed")
    val Succeed = Value(2, "Succeed")
    val Canceled = Value(3, "Canceled")

}
object ActionTracking {
    def apply(stepId: Int, action: Action) = new ActionTracking(stepId,
        action.desc, action.uuid, None, new Timestamp(System.currentTimeMillis()), status=ActionTrackingStatus.Executing)
}