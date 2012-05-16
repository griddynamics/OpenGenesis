/*
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

package com.griddynamics.genesis.users.service

import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.users.repository.LocalGroupRepository
import com.griddynamics.genesis.validation.Validation
import Validation._
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.api.{RequestResult, UserGroup}
import com.griddynamics.genesis.service.AuthorityService
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.repository.ProjectPropertyRepository

class LocalGroupService(val repository: LocalGroupRepository) extends GroupService with Validation[UserGroup] {
    @Autowired
    var authorityService: AuthorityService = null

    @Transactional(readOnly = true)
    def list = repository.list.sortBy(_.name)

    @Transactional(readOnly = true)
    def findByName(name: String) = repository.findByName(name)

    @Transactional(readOnly = true)
    def search(nameLike: String): List[UserGroup] = repository.search(nameLike)

    @Transactional
    override def create(a: UserGroup) = {
        validCreate(a, a => repository.insert(a))
    }

    @Transactional
    def create(a: UserGroup, users: List[String]) = {
        validCreate(a, a => {
            val newGroup = repository.insert(a)
            newGroup.id.map(i => users.map(u => repository.addUserToGroup(i, u)))
        })
    }

    @Transactional
    def update(group: UserGroup, users: List[String]) = {
      validUpdate(group, a => {
          val group = repository.update(a)
          repository.removeAllUsersFromGroup(group.id.get)
          group.id.map(i => users.map(u => repository.addUserToGroup(i, u)))
          group
      })
    }

    @Transactional(readOnly = true)
    def getUsersGroups(username: String) = repository.groupsForUser(username)

    @Transactional
    override def delete(a: UserGroup) = {
        authorityService.removeAuthoritiesFromGroup(a.name)
        repository.removeAllUsersFromGroup(a.id.get)
        repository.delete(a)
        RequestResult(isSuccess = true)
    }

    @Transactional(readOnly = true)
    def users(id: Int) = repository.usersForGroup(id) match {
        case None => Seq()
        case Some(list) => list
    }

    @Transactional
    def addUserToGroup(id: Int, username: String) = {
        repository.addUserToGroup(id, username)
        RequestResult(isSuccess = true)
    }

    def removeUserFromGroup(id: Int, username: String) = {
        repository.removeUserFromGroup(id, username)
        RequestResult(isSuccess = true)
    }

    def get(id: Int) = {
        repository.get(id)
    }

    protected def validateUpdate(c: UserGroup) = filterResults(Seq(
        notEmpty(c.name, "name"),
        notEmpty(c.description, "description"),
        mustExist(c){it => get(it.id.get)},
        must(c, "Group with name '" + c.name + "' already exists"){ c =>
          findByName(c.name) match {
              case None => true
              case Some(group) => group.id == c.id
        }
    }))

    protected def validateCreation(c: UserGroup) = filterResults(Seq(notEmpty(c.name, "name"),
        notEmpty(c.description, "description"), must(c, "name must be unique") {c => findByName(c.name).isEmpty}))
}
