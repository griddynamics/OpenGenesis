package com.griddynamics.genesis.repository

import com.griddynamics.genesis.model.PermissionChange

trait PermissionChangeRepository {
   def add(permissionChange: PermissionChange)
}
