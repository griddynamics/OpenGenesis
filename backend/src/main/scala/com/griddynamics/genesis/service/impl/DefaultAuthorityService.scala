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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service.AuthorityService
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model.Authority
import com.griddynamics.genesis.model.GenesisSchema.{userAuthorities, groupAuthorities}
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.users.GenesisRole._

class DefaultAuthorityService extends AuthorityService {

  val listAuthorities = List(SystemAdmin.toString, GenesisUser.toString)

  @Transactional
  def grantAuthoritiesToUser(username: String, auths: List[String]) = withValidRoles(auths) {
    userAuthorities.deleteWhere(item => item.principalName === username)

    val grantedAuths = auths.map { new Authority(username, _) }
    userAuthorities.insert(grantedAuths)

    new RequestResult(isSuccess = true)
  }

  @Transactional
  def grantAuthoritiesToGroup(groupName: String, auths: List[String]) = withValidRoles(auths) {
    groupAuthorities.deleteWhere(item => item.principalName === groupName)

    val grantedAuths = auths.map(new Authority(groupName, _))
    groupAuthorities.insert(grantedAuths)

    new RequestResult(isSuccess = true)
  }

  @Transactional
  def removeAuthoritiesFromUser(username: String) = {
    userAuthorities.deleteWhere(item => item.principalName === username)

    new RequestResult(isSuccess = true)
  }

  @Transactional
  def removeAuthoritiesFromGroup(groupName: String) = {
    groupAuthorities.deleteWhere(item => item.principalName === groupName)

    new RequestResult(isSuccess = true)
  }

  @Transactional(readOnly = true)
  def getUserAuthorities(username: String) =  from(userAuthorities) (item =>
    where(item.principalName === username) select (item.authority)
  ).toList

  @Transactional(readOnly = true)
  def getGroupAuthorities(groupName: String) = from(groupAuthorities) (item =>
    where(item.principalName === groupName) select (item.authority)
  ).toList

  @Transactional(readOnly = true)
  def getAuthorities(groups: Iterable[UserGroup]) = from(groupAuthorities)(item =>
    where(item.principalName in (groups.map(_.name))).select(item.authority)
  ).distinct.toList

  private def withValidRoles(auths: List[String])(block: => RequestResult) = {
    val unknownRoles: List[String] = auths.diff(listAuthorities)
    if(unknownRoles.isEmpty) {
      block
    } else {
      new RequestResult(isSuccess = false, compoundServiceErrors = List("Unknown authorities: [" + unknownRoles.mkString(",") + "]"))
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
    new RequestResult(isSuccess = true)
  }

//  @Transactional(readOnly = true)
//  def isUserProjectAdmin(username: String, groups: Iterable[UserGroup]):Boolean = {
//    val groupNames = groups.map ( "GROUP_" + _.name )
//    from(aclSid, aclEntry)((sid, entry) => where(
//        ((sid.principal === true and sid.sid === username) or (sid.principal === false and (sid.sid in (groupNames))))
//          and (sid.id === entry.sid and entry.mask === BasePermission.ADMINISTRATION.getMask )
//      ).select(entry.id)
//    ).size > 0
//  }
//
//  @Transactional(readOnly = true)
//  def getAllowedProjectIds(username: String, authorities: Iterable[String]): List[Int] = {
//    val sids = from(aclSid)(sid => where ((sid.principal === true and sid.sid === username) or (sid.principal === false and (sid.sid in authorities))) select (sid.id)).toList
//    val classId = from(aclClass)(clazz => where (clazz.`class` === classOf[Project].getCanonicalName) select (clazz.id)).head
//    from(aclEntry, aclObjectIdentity)((entry, oi) =>
//      where (
//        entry.acl_object_identity === oi.id and
//          oi.object_id_class === classId and
//          (entry.sid in sids) and
//          (entry.mask === BasePermission.ADMINISTRATION.getMask or entry.mask === BasePermission.READ.getMask)
//      ) select (oi.object_id_identity.toInt)
//    ).toList
//  }
//
//  @Transactional(readOnly = true)
//  def getProjectAuthority(projectId: Int, authorityName: String): ExtendedResult[(Iterable[String], Iterable[String])] = {   //todo why tuple?
//    if(!authorityPermissionMap.containsKey(authorityName)) {
//      return Failure(compoundServiceErrors = List("Name not found [" + authorityName + "]"))
//    }
//    val permission = authorityPermissionMap.getOrElse(authorityName, throw new IllegalArgumentException())
//    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
//    try {
//      val acls = aclService.readAclById(oi)
//      val (usersACE, groupsACE) = acls.getEntries.filter(_.getPermission == permission).partition(_.getSid.isInstanceOf[PrincipalSid])
//      val usersNames = usersACE.map { _.getSid.asInstanceOf[PrincipalSid].getPrincipal }
//      val groupNames = groupsACE.map { ace =>
//        val authority = ace.getSid.asInstanceOf[GrantedAuthoritySid].getGrantedAuthority
//        authority.replaceAll("^GROUP_", "")
//      }
//      Success((usersNames, groupNames))
//    } catch {
//      case e: NotFoundException => Success((List[String](), List[String]()))
//    }
//  }
//
//  @Transactional
//  def updateProjectAuthority(projectId: Int, authorityName: String, users: List[String], groups: List[String]): RequestResult = {
//    if(!authorityPermissionMap.containsKey(authorityName)){
//      return RequestResult(isSuccess = false, compoundServiceErrors = List("Name not found [" + authorityName + "]"))
//    }
//    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
//    val permission = authorityPermissionMap(authorityName)
//
//    val acl: MutableAcl = try {
//      val oldAcl = aclService.readAclById(oi).asInstanceOf[MutableAcl]
//      val entries = oldAcl.getEntries.filter(_.getPermission != permission)
//      aclService.deleteAcl(oi, false)
//      val newAcl = aclService.createAcl(oi)
//      entries.foreach(ace => newAcl.insertAce(newAcl.getEntries.size, ace.getPermission, ace.getSid, true))
//      newAcl
//    } catch {
//      case nfe: NotFoundException => aclService.createAcl(oi)
//    }
//
//    users.foreach { sid => acl.insertAce(acl.getEntries.size(), permission, new PrincipalSid(sid), true)}
//    groups.foreach { group => acl.insertAce(acl.getEntries.size(), permission,  new GrantedAuthoritySid("GROUP_" + group), true) }
//
//    aclService.updateAcl(acl)
//    RequestResult(isSuccess = true)
//  }
//
//  @Transactional(readOnly = true)
//  def getGrantedAuthorities(projectId: Int, username: String, authorities: Iterable[String]) = {
//    val sids: java.util.List[Sid] = List(new PrincipalSid(username)) ++ (authorities.filter(_.startsWith("GROUP_")).map { g => new GrantedAuthoritySid(g) })
//    try {
//      val acls = aclService.readAclById( new ObjectIdentityImpl(classOf[Project], projectId), sids)
//
//      val result = new ListBuffer[GenesisRole.Value]()
//
//      try {
//        val isUser: Boolean = acls.isGranted(Collections.singletonList(BasePermission.READ), sids, true)
//        if(isUser) {
//          result += GenesisRole.ProjectUser
//        }
//      } catch {
//        case e: NotFoundException => //do nothing
//      }
//
//     try {
//        val isAdmin = acls.isGranted(Collections.singletonList(BasePermission.ADMINISTRATION), sids, true)
//        if(isAdmin) {
//          result += GenesisRole.ProjectAdmin
//        }
//      } catch {
//        case e: NotFoundException => //do nothing
//      }
//
//      result.toList
//    } catch {
//      case e: NotFoundException => List()
//    }
//  }
}
