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

package com.griddynamics.genesis.groups

import com.griddynamics.genesis.common.CRUDService
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.api.{ExtendedResult, User, UserGroup}


trait GroupService extends CRUDService[UserGroup, String]{
    @Transactional(readOnly = true)
    override def get(name: String) = findByName(name)
    @Transactional(readOnly = true)
    def findByName(name: String) : Option[UserGroup]
    @Transactional(readOnly = true)
    def users(name: Int) : Seq[User]
    @Transactional(readOnly = false)
    def addUserToGroup(id: Int, username: String) : ExtendedResult[_]
    @Transactional(readOnly = false)
    def removeUserFromGroup(id: Int, username: String) : ExtendedResult[_]
    @Transactional(readOnly = true)
    def get(id: Int) : Option[UserGroup]
    @Transactional(readOnly = false)
    def create(a: UserGroup, users: List[String]) : ExtendedResult[UserGroup]
    @Transactional(readOnly = false)
    def update(group: UserGroup, users: List[String]) : ExtendedResult[UserGroup]
    @Transactional(readOnly = true)
    def getUsersGroups(username: String): Iterable[UserGroup]
    @Transactional(readOnly = false)
    def setUsersGroups(username: String, groups: Seq[String])
    @Transactional(readOnly = true)
    def search(nameLike: String): List[UserGroup]
    @Transactional(readOnly = true)
    def doesGroupExist(groupName: String): Boolean
    @Transactional(readOnly = true)
    def doGroupsExist(groupNames: Seq[String]): Boolean
    def isReadOnly = false
}

class GroupServiceStub extends GroupService {
    @Transactional(readOnly = true)
    def findByName(name: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def users(name: Int) = throw new UnsupportedOperationException

    @Transactional(readOnly = false)
    def addUserToGroup(id: Int, username: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = false)
    def removeUserFromGroup(id: Int, username: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def get(id: Int) = throw new UnsupportedOperationException

    @Transactional(readOnly = false)
    def create(a: UserGroup, users: List[String]) = throw new UnsupportedOperationException

    @Transactional(readOnly = false)
    def update(group: UserGroup, users: List[String]) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def getUsersGroups(username: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = false)
    def setUsersGroups(username: String, groups: Seq[String]) {throw new UnsupportedOperationException}

    @Transactional(readOnly = true)
    def search(nameLike: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def doesGroupExist(groupName: String) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def doGroupsExist(groupNames: Seq[String]) = throw new UnsupportedOperationException

    @Transactional(readOnly = true)
    def list = throw new UnsupportedOperationException

    override val isReadOnly = true
}


 object GroupServiceStub {
    private lazy val stub = new GroupServiceStub
    def get = stub
}