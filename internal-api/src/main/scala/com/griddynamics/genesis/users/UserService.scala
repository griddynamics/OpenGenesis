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
package com.griddynamics.genesis.users

import com.griddynamics.genesis.common.CRUDService
import com.griddynamics.genesis.api.User
import org.springframework.beans.factory.annotation.Value

trait UserService extends CRUDService[User, String] {

  @Value("genesis.system.admin.email") var adminEmail: String =_

  override def get(key: String) = findByUsername(key)

  def getWithCredentials(username: String): Option[User]

  def findByUsername(username: String): Option[User]

  def findByUsernames(userNames: Iterable[String]): Set[User]

  def search(usernameLike: String): List[User]

  def doesUserExist(userName: String): Boolean

  def doUsersExist(userNames: Iterable[String]): Boolean

  def isReadOnly = false
}

class UserServiceStub extends UserService {
  def getWithCredentials(username: String) = throw new UnsupportedOperationException

  def findByUsername(username: String) = throw new UnsupportedOperationException

  def findByUsernames(userNames: Iterable[String]) = throw new UnsupportedOperationException

  def search(usernameLike: String) = throw new UnsupportedOperationException

  def doesUserExist(userName: String) = throw new UnsupportedOperationException

  def doUsersExist(userNames: Iterable[String]) = throw new UnsupportedOperationException

  def list = throw new UnsupportedOperationException

  override val isReadOnly = true
}

object UserServiceStub {
  private lazy val stub = new UserServiceStub

  def get = stub
}