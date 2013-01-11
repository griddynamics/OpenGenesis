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

import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service.AuthorityService
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model.Authority
import com.griddynamics.genesis.model.GenesisSchema.{userAuthorities, groupAuthorities}
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.users.GenesisRole._

class DefaultAuthorityService(permissionService: PermissionService) extends AuthorityService {

  val listAuthorities = List(SystemAdmin.toString, ReadonlySystemAdmin.toString, GenesisUser.toString)

  @Transactional
  def grantAuthoritiesToUser(username: String, auths: List[String]) = withValidRoles(auths) {
    userAuthorities.deleteWhere(item => item.principalName === username)

    val grantedAuths = auths.map { new Authority(username, _) }
    userAuthorities.insert(grantedAuths)

    Success(username)
  }

  @Transactional
  def grantAuthoritiesToGroup(groupName: String, auths: List[String]) = withValidRoles(auths) {
    groupAuthorities.deleteWhere(item => item.principalName === groupName)

    val grantedAuths = auths.map(new Authority(groupName, _))
    groupAuthorities.insert(grantedAuths)

    Success(groupName)
  }

  @Transactional
  def removeAuthoritiesFromUser(username: String) = {
    userAuthorities.deleteWhere(item => item.principalName === username)
    permissionService.cleanUserPermissions(username)
    Success(username)
  }

  @Transactional
  def removeAuthoritiesFromGroup(groupName: String) = {
    groupAuthorities.deleteWhere(item => item.principalName === groupName)
    permissionService.cleanGroupPermissions(groupName)
    Success(groupName)
  }

  @Transactional(readOnly = true)
  def getUserAuthorities(username: String) =  from(userAuthorities) (item =>
    where(lower(item.principalName) === lower(username)) select (item.authority)
  ).toList

  @Transactional(readOnly = true)
  def getGroupAuthorities(groupName: String) = from(groupAuthorities) (item =>
    where(lower(item.principalName) === lower(groupName)) select (item.authority)
  ).toList

  @Transactional(readOnly = true)
  def getAuthorities(groups: Iterable[UserGroup]) = from(groupAuthorities)(item =>
    where(item.principalName in (groups.map(_.name))).select(item.authority)
  ).distinct.toList

  private def withValidRoles[A](auths: List[String])(block: => ExtendedResult[A]) = {
    val unknownRoles: List[String] = auths.diff(listAuthorities)
    if(unknownRoles.isEmpty) {
      block
    } else {
      Failure(compoundServiceErrors = List("Unknown authorities: [" + unknownRoles.mkString(",") + "]"))
    }
  }

  @Transactional(readOnly = true)
  def authorityAssociations(authorityName: String) = new AuthorityDescription (
    name = authorityName,
    groups = from(groupAuthorities)(item => where (item.authority === authorityName) select(item.principalName)).toList,
    users = from(userAuthorities)(item => where (item.authority === authorityName) select(item.principalName)).toList
  )

  @Transactional
  def updateAuthority(authorityName: String, groups: List[String], usernames: List[String]) = {
    groupAuthorities.deleteWhere(auth => auth.authority === authorityName)
    userAuthorities.deleteWhere(auth => auth.authority === authorityName)
    groups.foreach(group => groupAuthorities.insert(new Authority(group, authorityName)))
    usernames.foreach(user => userAuthorities.insert(new Authority(user, authorityName)))
    Success(None)
  }
}
