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
package com.griddynamics.genesis.ldap

import com.griddynamics.genesis.users.UserService
import org.springframework.ldap.core.{DirContextAdapter, ContextMapper, LdapTemplate}
import com.griddynamics.genesis.api.User
import scala.collection.JavaConversions._
import scala.util.control.Exception._
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator

trait LdapUserService extends UserService {
  def getUserGroups(username: String): Option[Seq[String]]
}

class LdapUserServiceImpl(val config: LdapPluginConfig,
                          val template: LdapTemplate,
                          val authoritiesPopulator: LdapAuthoritiesPopulator) extends LdapUserService {

  override def isReadOnly = true

  case class UserContextMapper(includeGroups: Boolean = true, includeCredentials: Boolean = false) extends ContextMapper {
    def mapFromContext(ctx: Any): User = {
      val adapter = ctx.asInstanceOf[DirContextAdapter]

      val principal =
        Option(config.addDomain(adapter.getStringAttribute(config.principalAttributeName))).getOrElse("UNKNOWN_USERNAME")

      val passwordOpt =
        if (includeCredentials)
          Option(adapter.getObjectAttribute("userPassword") match {
            case p: String => p
            case arr: Array[Byte] => new String(arr)
            case null => null
            case v => v.toString
          })
        else
          None

      val groupsOpt =
        if (includeGroups)
          Option(authoritiesPopulator.getGrantedAuthorities(adapter, principal).toSeq.map(_.getAuthority))
        else
          None

      User(
        principal,
        adapter.getStringAttribute("mail"),
        adapter.getStringAttribute("givenName"),
        adapter.getStringAttribute("sn"),
        Option(adapter.getStringAttribute("employeeType")),
        passwordOpt,
        groupsOpt
      )
    }
  }

  private def usernameFilter(pattern: String) = config.userSearchFilter.replace("{0}", pattern)

  private def filter(usernamePattern: String) =
    "(&(%s)(|(%s)(sn=%s)(givenName=%3$s)))"
      .format(config.usersServiceFilter, usernameFilter(usernamePattern), usernamePattern)

  private def find(username: String, includeCredentials: Boolean) =
    catching(classOf[IncorrectResultSizeDataAccessException]).opt(
      template.searchForObject(
        config.userSearchBase,
        filter(username),
        UserContextMapper(includeCredentials = includeCredentials)
      ).asInstanceOf[User]
    )

  def getWithCredentials(username: String): Option[User] =
    find(config.stripDomain(username), includeCredentials = true)

  def findByUsername(username: String): Option[User] =
    find(config.stripDomain(username), includeCredentials = false)

  def findByUsernames(userNames: Seq[String]): Seq[User] =
    template.search(
      config.userSearchBase,
      "(&(%s)(|%s))".format(
        config.usersServiceFilter,
        userNames.map { username => "(%s)".format(usernameFilter(config.stripDomain(username))) }.mkString
      ),
      UserContextMapper(includeGroups = false, includeCredentials = false)
    ).toList.asInstanceOf[List[User]].sortBy(_.username.toLowerCase)

  def search(usernameLike: String): List[User] =
    template.search(
      config.userSearchBase,
      filter(usernameLike),
      UserContextMapper(includeGroups = false)
    ).toList.asInstanceOf[List[User]].sortBy(_.username.toLowerCase)

  def doesUserExist(userName: String): Boolean =
    findByUsername(config.stripDomain(userName)).isDefined

  def doUsersExist(userNames: Seq[String]): Boolean =
    findByUsernames(userNames).map(_.username.toLowerCase).toSet == userNames.map(_.toLowerCase).toSet

  def list: List[User] =
    template.search(
      config.userSearchBase,
      config.usersServiceFilter,
      UserContextMapper(includeGroups = false)
    ).toList.asInstanceOf[List[User]].sortBy(_.username.toLowerCase)

  def getUserGroups(username: String): Option[Seq[String]] =
    find(config.stripDomain(username), includeCredentials = false) flatMap { _.groups }

}
