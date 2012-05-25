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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.users.service

import com.griddynamics.genesis.users.UserService
import org.springframework.transaction.annotation.{Propagation, Transactional}
import com.griddynamics.genesis.validation.Validation
import Validation._
import com.griddynamics.genesis.users.repository.{LocalGroupRepository, LocalUserRepository}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.AuthorityService
import com.griddynamics.genesis.api.{Success, Failure, User}

class LocalUserService(val repository: LocalUserRepository, val groupRepo: LocalGroupRepository) extends UserService with Validation[User]{
    @Autowired
    var authorityService: AuthorityService = null

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
    override def create(user: User, groups: List[String]) = {
        validCreate(user, user => {
            val newUser = repository.insert(user)
            groups.flatMap(groupRepo.findByName(_).flatMap(_.id)).foreach(groupRepo.addUserToGroup(_, newUser.username))
            newUser
        })
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def update(user: User) = {
       validUpdate(user, repository.update(_) )
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def delete(a: User) = {
      authorityService.removeAuthoritiesFromUser(a.username)
      if (repository.delete(a) == 0)
         Failure()
      else
         Success(a)
    }

    protected def validateUpdate(user: User) =
            mustExist(user){ it => repository.findByUsername(it.username) } ++
            mustMatchName(user, user.firstName, "firstName") ++
            mustMatchName(user, user.lastName, "lastName")++
            mustMatchEmail(user, user.email, "email") ++
            must(user, "Email [%s] is already registered for other user") {
                user => repository.findByEmail(user.email).filter(_.username != user.username).isDefined
            }

    protected def validateCreation(user: User) = {
            must(user, "User with username [" + user.username + "] is already registered") {
                user => repository.findByUsername(user.username).isEmpty
            } ++
            must(user, "User with email [%s] is already registered".format(user.email)) {
                user => repository.findByEmail(user.email).isEmpty
            } ++
            mustMatchUserName(user, user.username, "username") ++
            mustMatchName(user, user.firstName, "firstName") ++
            mustMatchName(user, user.lastName, "lastName") ++
            mustMatchEmail(user, user.email, "email") ++
            mustPresent(user, user.password, "password")
    }


    @Transactional(readOnly = true)
    def list = {
       repository.list
    }

    @Transactional(readOnly = true)
    def search(usernameLike: String) = repository.search(usernameLike)
}



