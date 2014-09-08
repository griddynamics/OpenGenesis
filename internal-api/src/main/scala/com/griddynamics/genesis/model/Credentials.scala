package com.griddynamics.genesis.model

class Credentials( val projectId: Option[Int] = None,
                   val cloudProvider: String,
                   val pairName: String,
                   val crIdentity: String, //note: some databases have 'identity' as reserved word
                   val credential: Option[String] = None,
                   val fingerPrint: Option[String] = None ) extends GenesisEntity