package com.griddynamics.genesis.model

import java.sql.Timestamp


class RemoteAgent(var agentId: GenesisEntity.Id,
                  var host: String,
                  var port: Int,
                  var tags: String,
                  var lastTimeAlive: Timestamp) extends GenesisEntity
