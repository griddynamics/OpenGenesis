package com.griddynamics.genesis.model

import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

class SquerylEntityAttr(val entityId: GenesisEntity.Id,
                        val name: String,
                        val value: String)
    extends KeyedEntity[CompositeKey2[GenesisEntity.Id, String]] {

    def this() = this (0, "", "")

    override def id = CompositeKey2(entityId, name)
}
