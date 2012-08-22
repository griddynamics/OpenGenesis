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
package com.griddynamics.genesis.users.service

import com.griddynamics.genesis.users.UserService
import org.springframework.transaction.annotation.{Propagation, Transactional}
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.users.repository.LocalUserRepository
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.api._

class LocalUserService(val repository: LocalUserRepository) extends UserService with Validation[User]{

  @Autowired
  var authorityService: AuthorityService = null

  @Autowired
  var groupService: GroupService = null

  @Transactional(readOnly = true)
  override def getWithCredentials(username: String) = repository.getWithCredentials(username)

  @Transactional(readOnly = true)
  override def findByUsername(username: String) = repository.findByUsername(username)

  @Transactional(readOnly = true)
  def list = repository.list

  @Transactional(readOnly = true)
  def search(usernameLike: String) = repository.search(usernameLike)

  @Transactional(readOnly = true)
  def doesUserExist(userName: String) = findByUsername(userName).isDefined

  @Transactional(readOnly = true)
  def doUsersExist(userNames: Seq[String]) = userNames.forall { doesUserExist(_) }

  @Transactional(readOnly = false)
  override def create(user: User) = {
    checkGroups(user.groups.getOrElse(Seq()), user) match {
      case f: Failure => f
      case Success(_,_) =>
        validCreate(user, repository.insert(_)) match {
          case s@Success(u, _) =>
              groupService.setUsersGroups(u.username, user.groups.getOrElse(Seq()))
              s
          case f => f
        }
    }
  }

  @Transactional(readOnly = false)
  override def update(user: User) = checkGroups(user.groups.getOrElse(Seq()), user) match {
    case f: Failure => f
    case _ => {
      validUpdate(user, repository.update(_)) match {
        case s@Success(u, _) =>
          groupService.setUsersGroups(user.username, user.groups.getOrElse(Seq()))
          s
        case f: Failure => f
      }
    }
  }

  @Transactional(readOnly = false)
  override def delete(a: User) = {
    authorityService.removeAuthoritiesFromUser(a.username)
    if (repository.delete(a) == 0)
       Failure()   //todo this will not rollback transaction
    else
       Success(a)
  }

  def checkGroups(groups: Seq[String], user : User): ExtendedResult[_] = {
    val unknownGroupNames = groups.filter { groupService.findByName(_).isEmpty }
    val failResults: Seq[ExtendedResult[_]] = unknownGroupNames.map { name =>
      Failure(isNotFound = true, serviceErrors = Map("groups" -> "Group %s does not exist".format(name)))
    }
    failResults.reduceLeftOption(_ ++ _).getOrElse(Success(None))
  }

  protected def validateUpdate(user: User) =
    mustExist(user){ it => repository.findByUsername(it.username) } ++
    must(user, "Email [%s] is already registered for other user".format(user.email)) {
        user => repository.findByEmail(user.email).filter(_.username != user.username).isEmpty
    }

  protected def validateCreation(user: User) = {
    must(user, "User with username [" + user.username + "] is already registered") {
        user => repository.findByUsername(user.username).isEmpty
    } ++
    must(user, "User with email [%s] is already registered".format(user.email)) {
        user => repository.findByEmail(user.email).isEmpty
    }
  }
}
