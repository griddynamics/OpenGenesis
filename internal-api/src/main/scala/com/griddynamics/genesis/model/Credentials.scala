package com.griddynamics.genesis.model

class Credentials( val projectId: GenesisEntity.Id,
                   val cloudProvider: String,
                   val pairName: String,
                   val crIdentity: String, //note: some databases have 'identity' as reserved word
                   val credential: Option[String],
                   val fingerPrint: Option[String] ) extends GenesisEntity with ProjectBoundEntity