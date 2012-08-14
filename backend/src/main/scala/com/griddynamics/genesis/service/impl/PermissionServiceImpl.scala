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

import org.springframework.transaction.annotation.Transactional
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model.GenesisSchema._
import org.springframework.security.acls.domain.{BasePermission, GrantedAuthoritySid, ObjectIdentityImpl, PrincipalSid}
import com.griddynamics.genesis.model.security.AclSid
import org.springframework.security.acls.model._
import collection.mutable.ListBuffer
import java.util.Collections

trait PermissionService {
  def cleanUserPermissions(username: String)

  def cleanGroupPermissions(groupname: String)

  def grantObjectPermission(oi: ObjectIdentity, permission: Permission, users: List[String], groups: List[String])

  def getPermissionAssignees(oi: ObjectIdentity, permission: Permission): (Iterable[String], Iterable[String])

  def getPermissions(oi: ObjectIdentity, sids: Seq[Sid]): Seq[Permission]

  def getPermittedIds(domainClazz: Class[_], username: String, authorities: Iterable[String], anyOf: Seq[Permission]): List[Int]
}

class PermissionServiceImpl(aclService: MutableAclService) extends PermissionService {

  val basepermissions = List(BasePermission.READ, BasePermission.ADMINISTRATION)

  @Transactional
  def cleanUserPermissions(username: String) {
    val aclSidOption = from(aclSid)(sid => where (sid.principal === true and sid.sid === username) select (sid)).headOption
    val apiSid = new PrincipalSid(username)

    aclSidOption.foreach { removeAccessControlEntries(_, apiSid) }
  }

  @Transactional
  def cleanGroupPermissions(groupname: String) {
    val sidName = "GROUP_" + groupname
    val aclSidOption = from(aclSid)(sid => where (sid.principal === false and sid.sid === sidName) select (sid)).headOption
    val apiSid = new GrantedAuthoritySid(sidName)

    aclSidOption.foreach { removeAccessControlEntries(_, apiSid) }
  }


  // NOTE: direct removal from aclEntry table is not performed because spring AclService is actually caching data, thus it's essential to modify entries via api
  private[this] def removeAccessControlEntries(aclSid: AclSid, apiSid: Sid) {
    import scala.collection.JavaConversions._
    import java.util.Collections._

    val projectIds = from(aclEntry, aclObjectIdentity, aclClass)((entry, oi, clazz) =>
      where(
          entry.sid === aclSid.id and
          oi.object_id_class === clazz.id and
          entry.acl_object_identity === oi.id
      ) select(oi.object_id_identity, clazz.`class`)
    ).toList

    val identities = projectIds.collect { case (id, name) => new ObjectIdentityImpl(Class.forName(name), id) }

    if(!identities.isEmpty) {
      val acls = aclService.readAclsById(identities, singletonList(apiSid))

      acls.foreach { case (oi, acl: MutableAcl) =>
        val notThisSidPermissions = acl.getEntries.filter(_.getSid != apiSid)

        for(_ <- 0 until acl.getEntries.size()) { acl.deleteAce(0) }

        notThisSidPermissions.foreach(ace => acl.insertAce(acl.getEntries.size, ace.getPermission, ace.getSid, true))
        aclService.updateAcl(acl)
      }
    }
  }


  @Transactional
  def grantObjectPermission(oi: ObjectIdentity, permission: Permission, users: List[String], groups: List[String]) {
    import scala.collection.JavaConversions._

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
  }


  @Transactional(readOnly = true)
  def getPermissionAssignees(oi: ObjectIdentity, permission: Permission): (Iterable[String], Iterable[String]) = {   //todo why tuple?
    import scala.collection.JavaConversions._

    try {
      val entries: Iterable[AccessControlEntry] = aclService.readAclById(oi).getEntries
      val (usersACE, groupsACE) = entries.filter(_.getPermission == permission).partition(_.getSid.isInstanceOf[PrincipalSid])
      val usersNames = usersACE.map { _.getSid.asInstanceOf[PrincipalSid].getPrincipal }
      val groupNames = groupsACE.map { ace =>
        val authority = ace.getSid.asInstanceOf[GrantedAuthoritySid].getGrantedAuthority
        authority.replaceAll("^GROUP_", "")
      }
      (usersNames, groupNames)
    } catch {
      case e: NotFoundException => (List[String](), List[String]())
    }
  }

  @Transactional(readOnly = true)
  def getPermissions(oi: ObjectIdentity, sids: Seq[Sid]): Seq[Permission] = {
    import scala.collection.JavaConversions._
    try {
      val acls = aclService.readAclById(oi, sids)

      val result = new ListBuffer[Permission]()

      basepermissions.foreach { perm =>
        try {
          if (acls.isGranted(Collections.singletonList(perm), sids, true)) {
            result += perm
          }
        } catch {
          case e: NotFoundException => //do nothing
        }
      }
      result
    } catch {
      case e: NotFoundException => List()
    }
  }

  @Transactional(readOnly = true)
  def getPermittedIds(domainClazz: Class[_], username: String, authorities: Iterable[String], anyOf: Seq[Permission]): List[Int] = {
    val sids = from(aclSid)(sid => where ((sid.principal === true and sid.sid === username) or (sid.principal === false and (sid.sid in authorities))) select (sid.id)).toList
    from(aclEntry, aclObjectIdentity, aclClass)((entry, oi, clazz) =>
      where (
        clazz.`class` === domainClazz.getCanonicalName and
          entry.acl_object_identity === oi.id and
          oi.object_id_class === clazz.id and
          (entry.sid in sids) and
          (entry.mask in anyOf.map(_.getMask))
      ) select (oi.object_id_identity.toInt)
    ).toList
  }
}
