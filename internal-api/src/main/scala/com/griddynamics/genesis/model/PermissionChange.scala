package com.griddynamics.genesis.model

import java.sql.Timestamp

class PermissionChange(var changedAt: Timestamp,
                        var changedBy: String,
                        var changeType: Changes.ChangesType,
                        var roleName: Option[String] = None,
                        var projectId: Option[Int] = None,
                        var confId: Option[Int] = None,
                        var payloadType: PermPayload.PermPayloadType,
                        var changedItem: String) extends GenesisEntity

object Changes extends Enumeration {
  type ChangesType = Value
  val Insert = Value(0, "Insert")
  val Delete = Value(1, "Delete")
}

object PermPayload extends Enumeration {
  type PermPayloadType = Value
  val User = Value(0, "User")
  val Group = Value(1, "Group")
}

