package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.api.{Configuration, Project, PermissionDiff}
import com.griddynamics.genesis.repository.PermissionChangeRepository
import com.griddynamics.genesis.model.{PermPayload, PermissionChange, Changes}
import java.util.Date
import java.sql.Timestamp
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.service.{EnvironmentConfigurationService, ProjectService}

class PermissionChangeService(repository: PermissionChangeRepository,
                              projectService: ProjectService,
                              confService: EnvironmentConfigurationService) extends service.PermissionChangeService {

  @Transactional(readOnly = false)
  def recordUserChanges(diff: PermissionDiff) {
    val projectAndConf = findProjectAndConfig(diff)
    save(diff, PermPayload.User, projectAndConf._1.map(_.name), projectAndConf._2.map(_.name))
  }


  def findProjectAndConfig(diff: PermissionDiff) = {
    val projectOpt: Option[Project] = diff.projectId.flatMap(projectService.get)
    val configOpt: Option[Configuration] = projectOpt.flatMap(project => {
      diff.confId.flatMap(cid => confService.get(project.id.get, cid))
    })
    (projectOpt, configOpt)
  }

  @Transactional(readOnly = false)
  def recordGroupChanges(diff: PermissionDiff) {
    val projectAndConf = findProjectAndConfig(diff)
    save(diff, PermPayload.Group, projectAndConf._1.map(_.name), projectAndConf._2.map(_.name))
  }


  def save(diff: PermissionDiff, payloadType: PermPayload.PermPayloadType, projectName: Option[String], confName: Option[String]) {
    PermissionChangeService.permissionRecords(diff, payloadType, projectName, confName).foreach(repository.add)
  }
}

object PermissionChangeService {
  def permissionRecords(diff: PermissionDiff, payloadType: PermPayload.PermPayloadType, projectName: Option[String], confName: Option[String]): Iterable[PermissionChange] = {
    val now = new Timestamp(new Date().getTime)
    (for (addition <- diff.added) yield permissionRecord(now, addition, Changes.Insert, payloadType, diff, projectName, confName)) ++
      (for (removal <- diff.removed) yield permissionRecord(now, removal, Changes.Delete, payloadType, diff, projectName, confName))
  }

  def permissionRecord(now: Timestamp, element: String, change: Changes.ChangesType,
                       payloadType: PermPayload.PermPayloadType,
                       diff: PermissionDiff, projectName: Option[String], confName: Option[String]) =
    new PermissionChange(now, diff.author, change, diff.roleName, diff.projectId, projectName, diff.confId, confName, payloadType, element)

}
