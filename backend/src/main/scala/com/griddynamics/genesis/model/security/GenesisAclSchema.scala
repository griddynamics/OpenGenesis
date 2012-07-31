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
package com.griddynamics.genesis.model.security

import org.squeryl.{KeyedEntity, Schema}

class AclSid(var id: Long, var principal: Boolean, var sid: String) extends KeyedEntity[Long]

class AclClass(var id: Long, var `class`: String) extends KeyedEntity[Long]

class AclObjectIdentity(var id: Long,
                        var object_id_class: Long,
                        var object_id_identity: Long,
                        var parent_object: Option[Long],
                        var owner_sid: Option[Long],
                        var entries_inheriting: Boolean) extends KeyedEntity[Long] {

  def this() = this(0, 0, 0, Some(0), Some(0), false)

}

class AclEntry(var id: Long,
               var acl_object_identity: Long,
               var ace_order: Int,
               var sid: Long,
               var mask: Int,
               var granting: Boolean,
               var audit_success: Boolean,
               var audit_failure: Boolean) extends KeyedEntity[Long]


trait GenesisAclSchema extends Schema {
  val aclSid = table[AclSid]("acl_sid")
  val aclClass = table[AclClass]("acl_class")
  val aclObjectIdentity = table[AclObjectIdentity]("acl_object_identity")
  val aclEntry = table[AclEntry]("acl_entry")

  import org.squeryl.PrimitiveTypeMode._

  on(aclSid)(sid => declare(
    sid.id is(primaryKey, autoIncremented),
    columns(sid.sid, sid.principal) are (unique)
  ))

  on(aclClass)(clazz => declare(
    clazz.id is (primaryKey, autoIncremented),
    clazz.`class` is (unique)
  ))

  on(aclObjectIdentity)(identity => declare(
    identity.id is(primaryKey, autoIncremented),
    columns(identity.object_id_class, identity.object_id_identity) are (unique)
  ))

  val objIdentToClass = oneToManyRelation(aclClass, aclObjectIdentity).via((clazz, aclObjIden) => clazz.id === aclObjIden.object_id_class)
  val objeIdenToSid = oneToManyRelation(aclSid, aclObjectIdentity).via((sid, identity) => sid.id === identity.owner_sid)

  on(aclEntry)(acl => declare(
    acl.id is(primaryKey, autoIncremented),
    columns(acl.acl_object_identity, acl.ace_order) are (unique)
  ))

  val aclToObjectIden = oneToManyRelation(aclObjectIdentity, aclEntry).via((identity, acl) => identity.id === acl.acl_object_identity)
  val aclToSid = oneToManyRelation(aclSid, aclEntry).via((sid, acl) => sid.id === acl.sid)
}

