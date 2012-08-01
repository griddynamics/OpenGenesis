/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.users.repository


import org.squeryl.Schema
import com.griddynamics.genesis.users.model.{UserToGroup, LocalGroup, LocalUser}

object LocalUserSchema extends LocalUserSchema with LocalUserPrimitiveSchema

trait LocalUserSchema extends Schema {
    val users = table[LocalUser]("local_users")
    val groups = table[LocalGroup]("local_group")
}

trait LocalUserPrimitiveSchema extends LocalUserSchema {

    import org.squeryl.PrimitiveTypeMode._

    on(users)(user => declare(
        user.username is (unique, dbType("varchar(64)")),
        user.email is (unique, dbType("varchar(256)")),
        user.pass is (dbType("varchar(64)")),
        user.firstName is (dbType("varchar(256)")),
        user.lastName is (dbType("varchar(256)")),
        user.jobTitle is (dbType("varchar(256)"))
    ))

    on(groups)(group => declare(
        group.name is (unique, dbType("varchar(64)")),
        group.description is (dbType("text")),
        group.mailingList is (dbType("text"))
    ))

    val userGroupsRelation = manyToManyRelation(users, groups).
      via[UserToGroup]((u, g, ug) => (ug.userId === u.id, ug.groupId === g.id))
}
