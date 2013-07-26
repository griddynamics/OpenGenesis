package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.{model, repository}
import com.griddynamics.genesis.annotation.RemoteGateway
import com.griddynamics.genesis.model.{GenesisSchema, PermissionChange}
import org.springframework.transaction.annotation.Transactional

@RemoteGateway("Genesis database access: PermissionChangeRepository")
class PermissionChangeRepository extends repository.PermissionChangeRepository {

  @Transactional(readOnly = false)
  def add(permissionChange: PermissionChange) {
    GenesisSchema.permissionChanges.insert(permissionChange)
  }
}
