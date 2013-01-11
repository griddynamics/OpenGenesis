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

import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService, ConfigService}
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import com.griddynamics.genesis.spring.security.{RoleBasedAuthority, AuthProviderFactory}
import org.springframework.security.ldap.userdetails.{LdapAuthoritiesPopulator, DefaultLdapAuthoritiesPopulator}
import org.springframework.security.ldap.authentication.{LdapAuthenticationProvider, BindAuthenticator}
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import com.griddynamics.genesis.service.GenesisSystemProperties._
import org.springframework.ldap.core.{DirContextOperations, LdapTemplate}
import scala.util.control.Exception._
import com.griddynamics.genesis.users.{GenesisRole, UserServiceStub}
import com.griddynamics.genesis.groups.GroupServiceStub
import scala.collection.JavaConversions._
import com.griddynamics.genesis.api.UserGroup
import java.util.concurrent.TimeUnit

@Configuration
@GenesisPlugin(id = "ldap", description = "Genesis LDAP Plugin")
class LdapPluginContext {

  @Autowired
  var configService : ConfigService = _
  @Autowired
  var authorityService: AuthorityService = _
  @Autowired
  var projectAuthorityService: ProjectAuthorityService = _

  lazy val config = new LdapPluginConfig(configService)

  lazy val contextSource = {
    val context = new DefaultSpringSecurityContextSource(config.serverUrl)
    config.base foreach { context.setBase(_) }
    config.managerDn foreach { context.setUserDn(_) }
    config.password foreach { context.setPassword(_) }
    context.setReferral("follow")
    context.afterPropertiesSet()
    context
  }

  lazy val ldapAuthoritiesPopulator = {
    val populator =
      new DefaultLdapAuthoritiesPopulator(contextSource, config.groupSearchBase)
    populator.setGroupSearchFilter(config.groupSearchFilter)
    populator.setSearchSubtree(true)
    populator.setConvertToUpperCase(false)
    populator.setRolePrefix("")
    populator
  }

  lazy val fullAuthoritiesPopulator = {
    new LdapAuthoritiesPopulator {
      def getGrantedAuthorities(userData: DirContextOperations, username: String) = {
        val groups =
          ldapAuthoritiesPopulator.getGrantedAuthorities(userData, username)
            .toList.map(authority => UserGroup(authority.getAuthority, null, None, None, None))
        var authorities = (
            authorityService.getAuthorities(groups) ++
            authorityService.getUserAuthorities(username) ++
            groups.map("GROUP_" + _.name) ++
            (if (projectAuthorityService.isUserProjectAdmin(username, groups.map(_.name))) List(GenesisRole.ProjectAdmin.toString) else List())
          )
        if(authorities.contains(GenesisRole.SystemAdmin.toString) || authorities.contains(GenesisRole.ReadonlySystemAdmin.toString)) {
          authorities = GenesisRole.GenesisUser.toString :: authorities
        }
        RoleBasedAuthority(authorities.distinct)
      }
    }
  }

  lazy val ldapTemplate = new LdapTemplate(contextSource)

  @Bean def ldapAuthProviderFactory = new AuthProviderFactory {
    val mode = "ldap"

    def create() = {
      val authenticator = new BindAuthenticator(contextSource)
      authenticator.setUserSearch(
        new FilterBasedLdapUserSearch(config.userSearchBase, config.userSearchFilter, contextSource)
      )
      authenticator.afterPropertiesSet()

      new LdapAuthenticationProvider(authenticator, fullAuthoritiesPopulator)
    }
  }

  @Bean def adAuthProviderFactory = new AuthProviderFactory {
    val mode = "ad"

    def create() =
      new ActiveDirectoryLdapAuthenticationProvider(config.domain.getOrElse(null), config.serverUrl)
  }

  @Bean def ldapUserService = catching(classOf[Exception]).opt {
    val service = new LdapUserServiceImpl(config, ldapTemplate, ldapAuthoritiesPopulator)
    service.findByUsername("username")
    service
  }.getOrElse(new UserServiceStub)

  @Bean def ldapGroupService = catching(classOf[Exception]).opt {
    val service = new LdapGroupServiceImpl(config, ldapTemplate, ldapUserService.asInstanceOf[LdapUserService])
    service.findByName("group")
    service
  }.getOrElse(new GroupServiceStub)

}

object LdapPluginContext {
  val PREFIX_LDAP = PLUGIN_PREFIX + ".ldap."
  val SERVER_URL = PREFIX_LDAP + "server.url"
  val BASE = PREFIX_LDAP + "base"
  val MANAGER_DN = PREFIX_LDAP + "manager.dn"
  val MANAGER_PASSWORD = PREFIX_LDAP + "manager.password"
  val DOMAIN = PREFIX_LDAP + "domain"
  val USER_SEARCH_FILTER = PREFIX_LDAP + "user.search.filter"
  val USER_SEARCH_BASE = PREFIX_LDAP + "user.search.base"
  val GROUP_SEARCH_FILTER = PREFIX_LDAP + "group.search.filter"
  val GROUP_SEARCH_BASE = PREFIX_LDAP + "group.search.base"
  val USERS_SERVICE_FILTER = PREFIX_LDAP + "users.service.filter"
  val GROUPS_SERVICE_FILTER = PREFIX_LDAP + "groups.service.filter"
  val SERVICE_DOMAIN_PREFIX = PREFIX_LDAP + "service.domain.prefix"
  val TIMEOUT = PREFIX_LDAP + "timeout"
  val SIZE_LIMIT = PREFIX_LDAP + "size.limit"
}

class LdapPluginConfig(val configService: ConfigService) {
  import LdapPluginContext._

  private val UserSearchFilter = """(?:.*\()?(\w+)=(?:.*)\{0\}(?:[^\)]*?)(?:\).*)?""".r

  private val GroupSearchFilter = """(?:.*\()?(\w+)=\{0\}(?:\).*)?""".r

  def serverUrl: String =
    configService.get(SERVER_URL) map { _.toString } getOrElse {
      throw new IllegalArgumentException("LDAP URL must be defined (%s)".format(SERVER_URL))
    }

  def base: Option[String] = configService.get(BASE).map(_.toString)

  def managerDn: Option[String] = configService.get(MANAGER_DN).map(_.toString)

  def password: Option[String] = configService.get(MANAGER_PASSWORD).map(_.toString)

  def domain: Option[String] = configService.get(DOMAIN).map(_.toString)

  def userSearchFilter: String = {
    val filter = configService.get(USER_SEARCH_FILTER, "")
    if (!filter.matches(UserSearchFilter.pattern.pattern()))
      throw new IllegalArgumentException("Invalid user search filter (%s)".format(filter))
    filter
  }

  def userSearchBase: String = configService.get(USER_SEARCH_BASE, "")

  def groupSearchFilter: String = {
    val filter = configService.get(GROUP_SEARCH_FILTER, "")
    if (!filter.matches(GroupSearchFilter.pattern.pattern()))
      throw new IllegalArgumentException("Invalid group search filter (%s)".format(filter))
    filter
  }

  def groupSearchBase: String = configService.get(GROUP_SEARCH_BASE, "")

  def usersServiceFilter: String = configService.get(USERS_SERVICE_FILTER) map { _.toString } getOrElse {
    throw new IllegalArgumentException("Users service filter must be defined (%s)".format(USERS_SERVICE_FILTER))
  }

  def groupsServiceFilter: String = configService.get(GROUPS_SERVICE_FILTER) map { _.toString } getOrElse {
    throw new IllegalArgumentException("Groups service filter must be defined (%s)".format(GROUPS_SERVICE_FILTER))
  }

  def principalAttributeName: String = {
    val UserSearchFilter(attrName) = userSearchFilter
    attrName
  }

  def groupMemberAttributeName: String = {
    val GroupSearchFilter(attrName) = groupSearchFilter
    attrName
  }

  private def serviceDomainPrefix: String = configService.get(SERVICE_DOMAIN_PREFIX, "") match {
    case s: String if !s.trim.isEmpty => s + "\\"
    case _ => ""
  }

  def stripDomain(str: String): String =
    Option(str) map { _.stripPrefix(serviceDomainPrefix) } getOrElse (str)

  def addDomain(str: String): String =
    Option(str) map { serviceDomainPrefix + _ } getOrElse (str)

  def timeout: Int = configService.get(TIMEOUT, TimeUnit.SECONDS.toMillis(30).toInt)

  def sizeLimit: Int = configService.get(SIZE_LIMIT, 500)

}
