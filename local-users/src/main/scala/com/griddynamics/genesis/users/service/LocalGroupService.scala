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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.users.service

import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.users.repository.LocalGroupRepository
import com.griddynamics.genesis.validation.Validation
import Validation._
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.service.AuthorityService
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.api.{Failure, ExtendedResult, Success, UserGroup}
import com.griddynamics.genesis.users.UserService

class LocalGroupService(val repository: LocalGroupRepository) extends GroupService with Validation[UserGroup] {

  @Autowired
  var authorityService: AuthorityService = null
  @Autowired
  var userService: UserService = _

  @Transactional(readOnly = true)
  def getUsersGroups(username: String) = repository.groupsForUser(username)

  @Transactional(readOnly = true)
  def list = repository.list.sortBy(_.name)

  @Transactional(readOnly = true)
  def findByName(name: String) = repository.findByName(name)

  @Transactional(readOnly = true)
  def findByNames(names: Iterable[String]) = repository.findByNames(names)

  @Transactional(readOnly = true)
  def search(nameLike: String): List[UserGroup] = repository.search(nameLike)

  @Transactional(readOnly = true)
  def get(id: Int) = repository.get(id)

  @Transactional(readOnly = true)
  def doesGroupExist(groupName: String) = findByName(groupName).isDefined

  @Transactional(readOnly = true)
  def doGroupsExist(groupNames: Iterable[String]) = groupNames.forall { doesGroupExist(_) }

  @Transactional
  override def create(a: UserGroup): ExtendedResult[UserGroup] = {
    val users = a.users.getOrElse(Seq())
    validCreate(a, a => {
      val newGroup = repository.insert(a)
      newGroup.id.map(i => users.map(u => repository.addUserToGroup(i, u)))
      newGroup
    })
  }

  @Transactional
  override def update(group: UserGroup): ExtendedResult[UserGroup] = {
    val users = group.users.getOrElse(Seq())
    validUpdate(group, a => {
      val group = repository.update(a)
      repository.removeAllUsersFromGroup(group.id.get)
      group.id.map(i => users.map(u => repository.addUserToGroup(i, u)))
      group
    })
  }

  @Transactional(readOnly = false)
  def setUsersGroups(username: String, groups: Seq[String]) {
    list.foreach{g =>
      g.id.map {
        if(groups.contains(g.name))
          addUserToGroup(_, username)
        else
          removeUserFromGroup(_, username)
      }
    }
  }

  @Transactional
  override def delete(a: UserGroup) = {
    authorityService.removeAuthoritiesFromGroup(a.name)
    repository.removeAllUsersFromGroup(a.id.get)
    repository.delete(a)
    Success(a)
  }

  @Transactional(readOnly = true)
  def users(id: Int) = repository.usersForGroup(id).getOrElse(Seq())

  @Transactional
  def addUserToGroup(id: Int, username: String) =  {
    if(!users(id).exists(_.username == username)) {
      repository.addUserToGroup(id, username)
    }
    Success((id, username))
  }

  @Transactional
  def removeUserFromGroup(id: Int, username: String) = {
    repository.removeUserFromGroup(id, username)
    Success((id, username))
  }

 protected def validateUpdate(c: UserGroup) =
  mustExist(c){it => get(it.id.get)} ++
  must(c, "Group name change is not allowed") { it => get(it.id.get).get.name == c.name } ++
  must(c, "Group with name '" + c.name + "' already exists"){ g =>
    findByName(g.name) match {
      case None => true
      case Some(group) => group.id == g.id
    }
  } ++
  validateUsers(c)

  protected def validateCreation(c: UserGroup) =
    must(c, "Group with name '" + c.name + "' already exists") {c => findByName(c.name).isEmpty} ++
    validateUsers(c)

  def validateUsers(c: UserGroup): ExtendedResult[UserGroup] = {
    if (!userService.doUsersExist(c.users.getOrElse(Seq()))) {
      Failure(compoundServiceErrors = Seq("Some of usernames in list is not found"), isNotFound = true)
    } else {
      Success(c)
    }
  }
}
