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
import com.griddynamics.genesis.users.repository.LocalUserRepository
import org.springframework.transaction.annotation.{Propagation, Transactional}
import com.griddynamics.genesis.api.{RequestResult, User}
import collection.Seq
import com.griddynamics.genesis.validation.Validation
import Validation._

class LocalUserService(val repository: LocalUserRepository) extends UserService with Validation[User]{
    @Transactional(readOnly = true)
    def findByUsername(username: String) = {
        repository.findByUsername(username)
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def create(user: User) : RequestResult = {
        validCreate(user, user => repository.insert(user))
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def update(user: User) : RequestResult = {
         validUpdate(user, user => {
             repository.findByUsername(user.username) match {
                 case None => RequestResult(isSuccess = false, compoundServiceErrors = Seq("Not found"))
                 case Some(lu) => {
                     repository.update(user)
                     RequestResult(isSuccess = true)
                 }
             }
         })
    }

    protected def validateUpdate(user: User) = {
        filterResults(Seq(
            mustMatchName(user.firstName, "firstName"),
            mustMatchName(user.lastName, "lastName"),
            mustMatchEmail(user.email, "email")
        ))
    }

    protected def validateCreation(user: User) = {
        filterResults(Seq(
            must(user) {
                user => repository.findByUsername(user.username).isEmpty
            },
            mustMatchUserName(user.username, "username"),
            mustMatchName(user.firstName, "firstName"),
            mustMatchName(user.lastName, "lastName"),
            mustMatchEmail(user.email, "email"),
            mustPresent(user.password, "password")
        ))
    }

    @Transactional(readOnly = true)
    def list = {
       repository.list
    }
}



