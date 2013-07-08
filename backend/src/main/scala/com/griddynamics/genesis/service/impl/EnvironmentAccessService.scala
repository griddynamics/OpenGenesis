/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service
import org.springframework.security.acls.domain.{GrantedAuthoritySid, PrincipalSid, BasePermission, ObjectIdentityImpl}
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.acls.model.Sid
import com.griddynamics.genesis.api.Configuration
import com.griddynamics.genesis.api.Environment
import com.griddynamics.genesis.users.GenesisRole

class EnvironmentAccessService(storeService: service.StoreService, permissionService: PermissionService) extends service.EnvironmentAccessService {

  @Value("${genesis.system.security.environment.restriction.enabled:false}")
  var securityEnabled: Boolean = _

  def getAccessGrantees(envId: Int): (Iterable[String], Iterable[String]) = {   //(usernames, groupnames)
    val oi = new ObjectIdentityImpl(classOf[Environment], envId)
    permissionService.getPermissionAssignees(oi, BasePermission.READ)
  }

  def grantAccess(envId: Int, users: Iterable[String], groups: Iterable[String]) {
    val oi = new ObjectIdentityImpl(classOf[Environment], envId)
    permissionService.grantObjectPermission(oi, BasePermission.READ, users.toList, groups.toList)
  }

  def getConfigAccessGrantees(id: Int): (Iterable[String], Iterable[String]) = {   //(usernames, groupnames)
  val oi = new ObjectIdentityImpl(classOf[Configuration], id)
    permissionService.getPermissionAssignees(oi, BasePermission.READ)
  }

  def grantConfigAccess(configId: Int, users: Iterable[String], groups: Iterable[String]) {
    val oi = new ObjectIdentityImpl(classOf[Configuration], configId)

    permissionService.grantObjectPermission(oi, BasePermission.READ, users.toList, groups.toList)

    val ids = storeService.findEnvsByConfigurationId(configId)
    ids.foreach( grantAccess(_, users, groups) )
  }

  private def sids(username: String, authorities: Iterable[String]): Seq[Sid] =
    new PrincipalSid(username) +: (authorities.filter(_.startsWith("GROUP_")).map { g => new GrantedAuthoritySid(g) }.toSeq)

  def hasAccessToAllConfigs(projectId: Int, username: String, authorities: Iterable[String]): Boolean = {
    if(!restrictionsEnabled) {
      return true
    }

    if (authorities.exists(_ == GenesisRole.SystemAdmin.toString)) {
      return true
    }

    if(permissionService.getPermissions(new ObjectIdentityImpl(classOf[Project], projectId), sids(username, authorities))
      .exists(_ == BasePermission.ADMINISTRATION)) {
      return true
    }
    false
  }

  def hasAccessToConfig(projectId: Int, configId: Int, username: String, authorities: Iterable[String]): Boolean = {
    if (hasAccessToAllConfigs(projectId, username, authorities)) return true
    val projectPerms = permissionService.getPermissions(new ObjectIdentityImpl(classOf[Project], projectId), sids(username, authorities))
    val perms = permissionService.getPermissions(new ObjectIdentityImpl(classOf[Configuration], configId), sids(username, authorities))

    projectPerms.exists(_ == BasePermission.READ) && perms.exists(_ == BasePermission.READ )
  }

  def restrictionsEnabled = securityEnabled

  def listAccessibleConfigurations(username: String, authorities: Iterable[String]) =
    permissionService.getPermittedIds(classOf[Configuration], username, authorities, Seq(BasePermission.READ))
}
