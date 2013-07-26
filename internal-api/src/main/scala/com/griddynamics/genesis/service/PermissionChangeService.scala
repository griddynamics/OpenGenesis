package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.PermissionDiff

trait PermissionChangeService {
   def recordUserChanges(diff: PermissionDiff)
   def recordGroupChanges(diff: PermissionDiff)
}
