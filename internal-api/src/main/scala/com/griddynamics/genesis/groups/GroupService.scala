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
import com.griddynamics.genesis.api.{ExtendedResult, User, UserGroup}

trait GroupService extends CRUDService[UserGroup, String]{
    override def get(name: String) = findByName(name)
    def findByName(name: String) : Option[UserGroup]
    def findByNames(names: Iterable[String]) : Set[UserGroup]
    def users(name: Int) : Seq[User]
    def addUserToGroup(id: Int, username: String) : ExtendedResult[_]
    def removeUserFromGroup(id: Int, username: String) : ExtendedResult[_]
    def get(id: Int) : Option[UserGroup]
    def getUsersGroups(username: String): Iterable[UserGroup]
    def setUsersGroups(username: String, groups: Seq[String])
    def search(nameLike: String): List[UserGroup]
    def doesGroupExist(groupName: String): Boolean
    def doGroupsExist(groupNames: Iterable[String]): Boolean
    def isReadOnly = false
}

class GroupServiceStub extends GroupService {
    def findByName(name: String) = throw new UnsupportedOperationException

    def findByNames(names: Iterable[String]) = throw new UnsupportedOperationException

    def users(name: Int) = throw new UnsupportedOperationException

    def addUserToGroup(id: Int, username: String) = throw new UnsupportedOperationException

    def removeUserFromGroup(id: Int, username: String) = throw new UnsupportedOperationException

    def get(id: Int) = throw new UnsupportedOperationException

    def getUsersGroups(username: String) = throw new UnsupportedOperationException

    def setUsersGroups(username: String, groups: Seq[String]) {throw new UnsupportedOperationException}

    def search(nameLike: String) = throw new UnsupportedOperationException

    def doesGroupExist(groupName: String) = throw new UnsupportedOperationException

    def doGroupsExist(groupNames: Iterable[String]) = throw new UnsupportedOperationException

    def list = throw new UnsupportedOperationException

    override val isReadOnly = true
}


 object GroupServiceStub {
    private lazy val stub = new GroupServiceStub
    def get = stub
}