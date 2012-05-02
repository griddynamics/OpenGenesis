package com.griddynamics.genesis.model

class Credentials( val projectId: GenesisEntity.Id,
                   val cloudProvider: String,
                   val pairName: String,
                   val identity: String,
                   val credential: Option[String],
                   val fingerPrint: Option[String]) extends GenesisEntity {

}