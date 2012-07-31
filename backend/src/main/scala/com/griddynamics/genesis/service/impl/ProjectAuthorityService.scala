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
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model.GenesisSchema._
import org.springframework.security.acls.domain.{GrantedAuthoritySid, PrincipalSid, ObjectIdentityImpl, BasePermission}
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.service
import collection.mutable.ListBuffer
import com.griddynamics.genesis.users.GenesisRole
import java.util.Collections
import com.griddynamics.genesis.users.GenesisRole._
import org.springframework.security.acls.model._
import scala.collection.JavaConversions._
import com.griddynamics.genesis.model.security.AclSid

class ProjectAuthorityService(aclService: MutableAclService) extends service.ProjectAuthorityService {

  val authorityPermissionMap = Map(ProjectAdmin -> BasePermission.ADMINISTRATION, ProjectUser -> BasePermission.READ)

  val projectAuthorities = authorityPermissionMap.keys

  @Transactional(readOnly = true)
  def isUserProjectAdmin(username: String, groups: Iterable[String]):Boolean = {
    val groupNames = groups.map ( "GROUP_" + _)
    from(aclSid, aclEntry)((sid, entry) => where(
      ((sid.principal === true and sid.sid === username) or (sid.principal === false and (sid.sid in groupNames)))
        and (sid.id === entry.sid and entry.mask === BasePermission.ADMINISTRATION.getMask)
    ).select(entry.id)
    ).size > 0
  }

  @Transactional(readOnly = true)
  def getAllowedProjectIds(username: String, authorities: Iterable[String]): List[Int] = {
    val sids = from(aclSid)(sid => where ((sid.principal === true and sid.sid === username) or (sid.principal === false and (sid.sid in authorities))) select (sid.id)).toList
    from(aclEntry, aclObjectIdentity, aclClass)((entry, oi, clazz) =>
      where (
        clazz.`class` === classOf[Project].getCanonicalName and
        entry.acl_object_identity === oi.id and
          oi.object_id_class === clazz.id and
          (entry.sid in sids) and
          (entry.mask === BasePermission.ADMINISTRATION.getMask or entry.mask === BasePermission.READ.getMask)
      ) select (oi.object_id_identity.toInt)
    ).toList
  }

  @Transactional(readOnly = true)
  def getProjectAuthority(projectId: Int, authorityName: GenesisRole.Value): ExtendedResult[(Iterable[String], Iterable[String])] = {   //todo why tuple?
    if(!authorityPermissionMap.contains(authorityName)) {
      return Failure(compoundServiceErrors = List("Name not found [" + authorityName + "]"))
    }
    val permission = authorityPermissionMap(authorityName)
    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
    try {
      val entries: Iterable[AccessControlEntry] = aclService.readAclById(oi).getEntries
      val (usersACE, groupsACE) = entries.filter(_.getPermission == permission).partition(_.getSid.isInstanceOf[PrincipalSid])
      val usersNames = usersACE.map { _.getSid.asInstanceOf[PrincipalSid].getPrincipal }
      val groupNames = groupsACE.map { ace =>
        val authority = ace.getSid.asInstanceOf[GrantedAuthoritySid].getGrantedAuthority
        authority.replaceAll("^GROUP_", "")
      }
      Success((usersNames, groupNames))
    } catch {
      case e: NotFoundException => Success((List[String](), List[String]()))
    }
  }

  @Transactional
  def updateProjectAuthority(projectId: Int, authorityName: GenesisRole.Value, users: List[String], groups: List[String]): ExtendedResult[_] = {
    if(!authorityPermissionMap.contains(authorityName)){
      return Failure(compoundServiceErrors = List("Name not found [" + authorityName + "]"))
    }
    val oi = new ObjectIdentityImpl(classOf[Project], projectId)
    val permission = authorityPermissionMap(authorityName)

    val acl: MutableAcl = try {
      val oldAcl: Iterable[AccessControlEntry] = aclService.readAclById(oi).getEntries
      val entries = oldAcl.filter(_.getPermission != permission)
      aclService.deleteAcl(oi, false)
      val newAcl = aclService.createAcl(oi)
      entries.foreach(ace => newAcl.insertAce(newAcl.getEntries.size, ace.getPermission, ace.getSid, true))
      newAcl
    } catch {
      case nfe: NotFoundException => aclService.createAcl(oi)
    }

    users.foreach { sid => acl.insertAce(acl.getEntries.size(), permission, new PrincipalSid(sid), true)}
    groups.foreach { group => acl.insertAce(acl.getEntries.size(), permission,  new GrantedAuthoritySid("GROUP_" + group), true) }

    aclService.updateAcl(acl)
    Success(None)
  }


  @Transactional
  def removeUserFromProjects(username: String) {
    val aclSidOption = from(aclSid)(sid => where (sid.principal === true and sid.sid === username) select (sid)).headOption
    val apiSid = new PrincipalSid(username)

    aclSidOption.foreach { removeAccessControlEntries(_, apiSid) }
  }

  // NOTE: direct removal from aclEntry table is not performed because spring AclService is actually caching data, thus it's essential to modify entries via api
  private[this] def removeAccessControlEntries(aclSid: AclSid, apiSid: Sid) {
    import scala.collection.JavaConversions._
    import java.util.Collections._

    val projectIds = from(aclEntry, aclObjectIdentity, aclClass)((entry, oi, clazz) =>
      where(
        clazz.`class` === classOf[Project].getCanonicalName and
          entry.sid === aclSid.id and
          oi.object_id_class === clazz.id and
          entry.acl_object_identity === oi.id
      ) select(oi.object_id_identity)
    ).toList

    val projectIdentities = projectIds.map { new ObjectIdentityImpl(classOf[Project], _).asInstanceOf[ObjectIdentity] }

    if(!projectIdentities.isEmpty) {
      val acls = aclService.readAclsById(projectIdentities, singletonList(apiSid))

      acls.foreach { case (oi, acl: MutableAcl) =>
        val notThisSidPermissions = acl.getEntries.filter(_.getSid != apiSid)

        for(_ <- 0 until acl.getEntries.size()) { acl.deleteAce(0) }

        notThisSidPermissions.foreach(ace => acl.insertAce(acl.getEntries.size, ace.getPermission, ace.getSid, true))
        aclService.updateAcl(acl)
      }
    }
  }

  @Transactional
  def removeGroupFromProjects(groupname: String) {
    val sidName = "GROUP_" + groupname
    val aclSidOption = from(aclSid)(sid => where (sid.principal === false and sid.sid === sidName) select (sid)).headOption
    val apiSid = new GrantedAuthoritySid(sidName)

    aclSidOption.foreach { removeAccessControlEntries(_, apiSid) }
  }

  @Transactional(readOnly = true)
  def getGrantedAuthorities(projectId: Int, username: String, authorities: Iterable[String]) = {
    val sids: java.util.List[Sid] = List(new PrincipalSid(username)) ++ (authorities.filter(_.startsWith("GROUP_")).map { g => new GrantedAuthoritySid(g) })
    try {
      val acls = aclService.readAclById( new ObjectIdentityImpl(classOf[Project], projectId), sids)

      val result = new ListBuffer[GenesisRole.Value]()

      try {
        val isUser: Boolean = acls.isGranted(Collections.singletonList(BasePermission.READ), sids, true)
        if(isUser) {
          result += GenesisRole.ProjectUser
        }
      } catch {
        case e: NotFoundException => //do nothing
      }

      try {
        val isAdmin = acls.isGranted(Collections.singletonList(BasePermission.ADMINISTRATION), sids, true)
        if(isAdmin) {
          result += GenesisRole.ProjectAdmin
        }
      } catch {
        case e: NotFoundException => //do nothing
      }

      result.toList
    } catch {
      case e: NotFoundException => List()
    }
  }
}
