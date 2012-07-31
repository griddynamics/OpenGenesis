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

class LocalUserService(val repository: LocalUserRepository, val groupService: GroupService) extends UserService with Validation[User]{

    @Autowired
    var authorityService: AuthorityService = null

    @Autowired
    var projectAuthorityService: ProjectAuthorityService = null

    @Transactional(readOnly = true)
    override def getWithCredentials(username: String) = repository.getWithCredentials(username)

    @Transactional(readOnly = true)
    override def findByUsername(username: String) = {
        repository.findByUsername(username)
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def create(user: User)  = {
        validCreate(user, u => repository.insert(u))
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def create(user: User, groups: Seq[String]) = {
        checkGroups(groups, user) match {
            case f: Failure => f
            case Success(_,_) =>
                create(user) match {
                    case s@Success(u, _) =>
                        groupService.setUsersGroups(u.username, groups)
                        s
                    case f => f
                }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def update(user: User) = {
       validUpdate(user, repository.update(_) )
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def update(user: User, groups: Seq[String]) = checkGroups(groups, user) match {
        case f: Failure => f
        case _ => {
            update(user) match {
                case s@Success(u, _) =>
                    groupService.setUsersGroups(user.username, groups)
                    s
                case f => f
            }
        }
    }


    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def delete(a: User) = {
      authorityService.removeAuthoritiesFromUser(a.username)
      projectAuthorityService.removeUserFromProjects(a.username)
      if (repository.delete(a) == 0)
         Failure()
      else
         Success(a)
    }

    def checkGroups(groups: Seq[String], user : User) = {
        groups.map(name => {
            groupService.findByName(name) match {
            case Some(g) => Success(user)
            case None => Failure(isNotFound = true,
                serviceErrors = Map("groups" -> "Group %s does not exist".format(name)))
        }}).reduceLeftOption(_ ++ _) match {
            case Some(r) => r
            case None => Success(None)
        }
    }

    protected def validateUpdate(user: User) =
            mustExist(user){ it => repository.findByUsername(it.username) } ++
            mustMatchPersonName(user, user.firstName, "First Name") ++
            mustMatchPersonName(user, user.lastName, "Last Name") ++
            mustMatchEmail(user, user.email, "E-Mail") ++
            must(user, "Email [%s] is already registered for other user".format(user.email)) {
                user => repository.findByEmail(user.email).filter(_.username != user.username).isEmpty
            } ++
            mustSatisfyLengthConstraints(user, user.jobTitle.getOrElse(""), "Job Title")(0, 128)

    protected def validateCreation(user: User) = {
            must(user, "User with username [" + user.username + "] is already registered") {
                user => repository.findByUsername(user.username).isEmpty
            } ++
            must(user, "User with email [%s] is already registered".format(user.email)) {
                user => repository.findByEmail(user.email).isEmpty
            } ++
            mustMatchName(user, user.username, "User name") ++
            mustMatchPersonName(user, user.firstName, "First Name") ++
            mustMatchPersonName(user, user.lastName, "Last Name") ++
            mustMatchEmail(user, user.email, "E-Mail") ++
            notEmpty(user, user.password.getOrElse(""), "Password") ++
            mustSatisfyLengthConstraints(user, user.jobTitle.getOrElse(""), "Job Title")(0, 128)
    }


    @Transactional(readOnly = true)
    def list = {
       repository.list
    }

    @Transactional(readOnly = true)
    def search(usernameLike: String) = repository.search(usernameLike)

    @Transactional(readOnly = true)
    def doesUserExist(userName: String) = findByUsername(userName).isDefined

    @Transactional(readOnly = true)
    def doUsersExist(userNames: Seq[String]) = userNames.forall { doesUserExist(_) }
}
