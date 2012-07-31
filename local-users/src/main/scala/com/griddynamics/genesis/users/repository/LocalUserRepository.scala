package com.griddynamics.genesis.users.repository

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

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.users.model.LocalUser
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.api.User


class LocalUserRepository extends AbstractGenericRepository[LocalUser, User](LocalUserSchema.users) {

  def search(nameLike: String): List[User] = from(table)(
    user => where(user.username like nameLike.replaceAll("\\*", "%") and user.deleted === false).select (user)
  ).toList.map(convert _)

  def getWithCredentials(username: String): Option[User] = {
        from (LocalUserSchema.users) {
          item => where (item.username === username and item.deleted === false).
            select(item)
        }.headOption.map(LocalUserRepository.convertWithoutStrippingPassword _)
    }


    def findByUsername(s: String): Option[User] = {
        from (LocalUserSchema.users) {
            item => where (
                item.username === s and
                item.deleted === false
            ).select(item)
        }.headOption match {
            case Some(user) => Some(convert(user))
            case None => None
        }
    }

    def findByEmail(email: String) : Option[User] = {
        from (LocalUserSchema.users) {
            item => where (
                item.email  === email
            ).select(item)
        }.headOption.map(convert(_))
    }

    override def update(user: User) : User = {
        table.update(
            u => where (u.username === user.username)
              set(
              u.email := user.email,
              u.firstName := user.firstName,
              u.lastName := user.lastName,
              u.jobTitle := user.jobTitle)
        )
        user
    }

    override def delete (user: User) : Int = {
        table.update (
           u => where(u.username  === user.username)
           set(
             u.deleted := true,
             u.username := user.username + " [deleted at %d]".format(System.currentTimeMillis()),
             u.email := user.email + " [deleted at %d]".format(System.currentTimeMillis())
           )
        )
    }

    override def list =
        from (LocalUserSchema.users) {
            item => where (
                item.deleted === false
            ).select(item)
        }.toList.map(convert(_))

    implicit def convert(model: LocalUser) = LocalUserRepository.convert(model)
    implicit def convert(dto: User) =  LocalUserRepository.convert(dto)
}

object LocalUserRepository {

    implicit def convertWithoutStrippingPassword(model: LocalUser) = {
        User(model.username, model.email, model.firstName, model.lastName, model.jobTitle, Option(model.password))
    }

    implicit def convert(model: LocalUser) = {
        User(model.username, model.email, model.firstName, model.lastName, model.jobTitle, None)
    }

    implicit def convert(dto: User) = {
        LocalUser(dto.username, dto.email, dto.firstName, dto.lastName, dto.jobTitle, dto.password)
    }
}
