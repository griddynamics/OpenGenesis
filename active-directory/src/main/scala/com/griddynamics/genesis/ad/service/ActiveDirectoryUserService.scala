/**
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
package com.griddynamics.genesis.ad.service

import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.util.Logging
import com4j.typelibs.ado20.Fields
import scala.None
import com.griddynamics.genesis.ad._
import com.griddynamics.genesis.api.User

trait ActiveDirectoryUserService extends UserService

class ActiveDirectoryUserServiceImpl(val namingContext: String,
                                     val pluginConfig: ActiveDirectoryPluginConfig,
                                     val template: CommandTemplate) extends AbstractActiveDirectoryService with ActiveDirectoryUserService with Logging {

  override def isReadOnly = true

  object UserMapper extends FieldsMapper[User] with MappingUtils {
    protected val config = pluginConfig

    def mapFromFields(fields: Fields) = {
      User(
        getAccountName(fields),
        getStringField("mail", fields).getOrElse(""),
        getStringField("givenName", fields).getOrElse(""),
        getStringField("sn", fields).getOrElse(""),
        None,
        None,
        None
      )
    }

  }

  case class Query(override val filter: String)
    extends Command(namingContext, "(&(%s)(sAMAccountType=805306368))".format(filter), "distinguishedName,sAMAccountName,sn,givenName,mail", "subTree")

  def getWithCredentials(username: String) = throw new UnsupportedOperationException

  def findByUsername(username: String) =
    template.query(Query("(sAMAccountName=%s)".format(normalize(username))), UserMapper).headOption

  def findByUsernames(userNames: Iterable[String]): Set[User] = {
    if (userNames.isEmpty)
      return Set()

    val filter = "(|%s)".format(
      userNames.map { username => "(sAMAccountName=%s)".format(normalize(username)) }.mkString
    )
    template.query(Query(filter), UserMapper).toSet
  }

  def search(usernameLike: String) = {
    val filter =
      if (usernameLike.matches("^[^ ]+ [^ ]+$")) {
        val compositeName = usernameLike.split("(?<!^) ", 2)
        "(&(givenName=%1$s)(sn=%2$s))".format(escape(compositeName(0)), escape(compositeName(1)))
      } else
        "(|(sAMAccountName=%1$s)(sn=%1$s)(givenName=%1$s))".format(escape(usernameLike))

    template.query(Query(filter), UserMapper).toList
  }

  def doesUserExist(userName: String) = findByUsername(userName).isDefined

  def doUsersExist(userNames: Iterable[String]) =
    findByUsernames(userNames).map(_.username.toLowerCase).toSet == userNames.map(_.toLowerCase).toSet

  def list = template.query(Query("(*)"), UserMapper)
}
