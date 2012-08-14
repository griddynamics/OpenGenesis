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
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.service.impl

import org.springframework.transaction.annotation.Transactional
import org.springframework.security.acls.domain.{GrantedAuthoritySid, PrincipalSid, ObjectIdentityImpl, BasePermission}
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service
import com.griddynamics.genesis.users.GenesisRole
import com.griddynamics.genesis.users.GenesisRole._
import org.springframework.security.acls.model._

class ProjectAuthorityService(permissionService: PermissionService) extends service.ProjectAuthorityService {

  val authorityPermissionMap = Map(ProjectAdmin -> BasePermission.ADMINISTRATION, ProjectUser -> BasePermission.READ)

  val projectAuthorities = authorityPermissionMap.keys

  @Transactional(readOnly = true)
  def isUserProjectAdmin(username: String, groups: Iterable[String]):Boolean = {
    val groupNames = groups.map ( "GROUP_" + _)
    val ids = permissionService.getPermittedIds(classOf[Project], username, groupNames, Seq(BasePermission.ADMINISTRATION))
    ids.nonEmpty
  }

  @Transactional(readOnly = true)
  def getAllowedProjectIds(username: String, authorities: Iterable[String]): List[Int] = {
    permissionService.getPermittedIds(classOf[Project], username, authorities, Seq(BasePermission.READ, BasePermission.ADMINISTRATION))
  }

  @Transactional(readOnly = true)
  def getProjectAuthority(projectId: Int, authorityName: GenesisRole.Value): ExtendedResult[(Iterable[String], Iterable[String])] = {
    if(!authorityPermissionMap.contains(authorityName)) {
      return Failure(compoundServiceErrors = List("Name not found [" + authorityName + "]"))
    }
    val permission = authorityPermissionMap(authorityName)

    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
    Success(permissionService.getPermissionAssignees(oi, permission))
  }

  @Transactional
  def updateProjectAuthority(projectId: Int, authorityName: GenesisRole.Value, users: List[String], groups: List[String]): ExtendedResult[_] = {
    if(!authorityPermissionMap.contains(authorityName)){
      return Failure(compoundServiceErrors = List("Name not found [" + authorityName + "]"))
    }
    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
    val permission = authorityPermissionMap(authorityName)

    permissionService.grantObjectPermission(oi, permission, users, groups)
    Success(None)
  }

  @Transactional(readOnly = true)
  def getGrantedAuthorities(projectId: Int, username: String, authorities: Iterable[String]) = {
    val sids: Seq[Sid] = new PrincipalSid(username) +: (authorities.filter(_.startsWith("GROUP_")).map { g => new GrantedAuthoritySid(g) }.toSeq)

    val perms = permissionService.getPermissions(new ObjectIdentityImpl(classOf[Project], projectId), sids)

    perms.collect { authorityPermissionMap.map (_.swap) }
  }
}
