package com.griddynamics.genesis.model

class Credentials( val projectId: GenesisEntity.Id,
                   val cloudProvider: String,
                   val pairName: String,
                   val identity: String,
                   val credential: String ) extends GenesisEntity {


}