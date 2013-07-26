package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.api.PermissionDiff
import com.griddynamics.genesis.repository.PermissionChangeRepository
import com.griddynamics.genesis.model.{PermPayload, PermissionChange, Changes}
import java.util.Date
import java.sql.Timestamp
import org.springframework.transaction.annotation.Transactional

class PermissionChangeService(repository: PermissionChangeRepository) extends service.PermissionChangeService {

  @Transactional(readOnly = false)
  def recordUserChanges(diff: PermissionDiff) {
    save(diff, PermPayload.User)
  }

  @Transactional(readOnly = false)
  def recordGroupChanges(diff: PermissionDiff) {
    save(diff, PermPayload.Group)
  }


  def save(diff: PermissionDiff, payloadType: PermPayload.PermPayloadType) {
    PermissionChangeService.permissionRecords(diff, payloadType).foreach(repository.add)
  }
}

object PermissionChangeService {
  def permissionRecords(diff: PermissionDiff, payloadType: PermPayload.PermPayloadType): Iterable[PermissionChange] = {
    val now = new Timestamp(new Date().getTime)
    (for (addition <- diff.added) yield permissionRecord(now, addition, Changes.Insert, payloadType, diff)) ++
      (for (removal <- diff.removed) yield permissionRecord(now, removal, Changes.Delete, payloadType, diff))
  }

  def permissionRecord(now: Timestamp, element: String, change: Changes.ChangesType,
                       payloadType: PermPayload.PermPayloadType,
                       diff: PermissionDiff) =
    new PermissionChange(now, diff.author, change, diff.roleName, diff.projectId, diff.confId, payloadType, element)

}
