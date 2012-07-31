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
package com.griddynamics.genesis.users.model


import org.apache.commons.codec.digest.DigestUtils
import com.griddynamics.genesis.users.GenesisUser
import com.griddynamics.genesis.model.GenesisEntity
import com.griddynamics.genesis.users.repository.LocalUserSchema


class LocalUser(val username: String, val email: String, val firstName: String, val lastName: String,
                val jobTitle: Option[String], val deleted: Boolean = false) extends GenesisUser with GenesisEntity {
    def this() = this("", "", "", "", None)
    var pass: String = _

    def password_=(pass: String) {
        this.pass = DigestUtils.sha256Hex(pass)
    }

    def password = {
        this.pass
    }

    lazy val groups = LocalUserSchema.userGroupsRelation.left(this)
}

object LocalUser {
    def apply(username: String, email: String, firstName: String, lastName: String, jobTitle: Option[String], password: Option[String]) = {
        val user = new LocalUser(username, email, firstName, lastName, jobTitle)
        password.map(user.password = _)
        user
    }
}
