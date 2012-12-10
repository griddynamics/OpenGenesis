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
import com.griddynamics.genesis.service.ConfigService
import scala.util.control.Exception._
import org.springframework.dao.IncorrectResultSizeDataAccessException
import LdapPluginContext._
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator

trait LdapUserService extends UserService {
  def getUserGroups(username: String): Option[Seq[String]]
}

class LdapUserServiceImpl(val configService: ConfigService,
                          val template: LdapTemplate,
                          val authoritiesPopulator: LdapAuthoritiesPopulator) extends LdapUserService {

  case class UserContextMapper(includeGroups: Boolean = true, includeCredentials: Boolean = false) extends ContextMapper {
    def mapFromContext(ctx: Any): User = {
      val adapter = ctx.asInstanceOf[DirContextAdapter]
      val username = adapter.getStringAttribute("uid")
      User(
        username,
        adapter.getStringAttribute("mail"),
        adapter.getStringAttribute("givenName"),
        adapter.getStringAttribute("sn"),
        Option(adapter.getStringAttribute("employeeType")),
        if (includeCredentials) Option(adapter.getStringAttribute("userPassword")) else None,
        if (includeGroups)
          Option(authoritiesPopulator.getGrantedAuthorities(adapter, username).toSeq.map(_.getAuthority))
        else
          None
      )
    }
  }

  private def filter(usernamePattern: String) =
    "(&%s(uid=%s))".format(configService.get(USERS_SERVICE_FILTER, ""), usernamePattern)

  private def find(username: String, includeCredentials: Boolean) =
    catching(classOf[IncorrectResultSizeDataAccessException]).opt(
      template.searchForObject(
        configService.get(USER_SEARCH_BASE, ""),
        filter(username),
        UserContextMapper(includeCredentials = includeCredentials)
      ).asInstanceOf[User]
    )

  def getWithCredentials(username: String): Option[User] = find(username, includeCredentials = true)

  def findByUsername(username: String): Option[User] = find(username, includeCredentials = false)

  def search(usernameLike: String): List[User] =
    template.search(
      configService.get(USER_SEARCH_BASE, ""),
      filter(usernameLike),
      UserContextMapper(includeGroups = false)
    ).toList.asInstanceOf[List[User]]

  def doesUserExist(userName: String): Boolean = findByUsername(userName).isDefined

  def doUsersExist(userNames: Seq[String]): Boolean = userNames.forall { doesUserExist(_) }

  def list: List[User] =
    template.search(
      configService.get(USER_SEARCH_BASE, ""),
      configService.get(USERS_SERVICE_FILTER, ""),
      UserContextMapper(includeGroups = false)
    ).toList.asInstanceOf[List[User]]

  def getUserGroups(username: String): Option[Seq[String]] =
    find(username, includeCredentials = false) flatMap { _.groups }

}
