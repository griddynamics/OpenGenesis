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
import com.griddynamics.genesis.spring.security.{RoleBasedAuthority, GenesisUserDetailsService, AuthProviderFactory}
import org.springframework.security.ldap.userdetails.{LdapAuthoritiesPopulator, DefaultLdapAuthoritiesPopulator}
import org.springframework.security.ldap.authentication.{LdapAuthenticationProvider, BindAuthenticator}
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import com.griddynamics.genesis.service.GenesisSystemProperties._
import org.springframework.ldap.core.{DirContextOperations, LdapTemplate}
import scala.util.control.Exception._
import com.griddynamics.genesis.users.{GenesisRole, UserService, UserServiceStub}
import com.griddynamics.genesis.groups.GroupServiceStub
import scala.collection.JavaConversions._
import com.griddynamics.genesis.api.UserGroup

@Configuration
@GenesisPlugin(id = "ldap", description = "Genesis LDAP Plugin")
class LdapPluginContext {
  import LdapPluginContext._

  @Autowired
  var configService : ConfigService = _
  @Autowired
  var authorityService: AuthorityService = _
  @Autowired
  var projectAuthorityService: ProjectAuthorityService = _

  lazy val serverUrl: String =
    configService.get(SERVER_URL) map { _.toString } getOrElse {
      throw new IllegalArgumentException("LDAP URL must be defined (%s)".format(SERVER_URL))
    }

  lazy val contextSource = {
    val context = new DefaultSpringSecurityContextSource(serverUrl)
    configService.get(BASE) foreach { s => context.setBase(s.toString)}
    configService.get(MANAGER_DN) foreach { s => context.setUserDn(s.toString) }
    configService.get(MANAGER_PASSWORD) foreach { s => context.setPassword(s.toString) }
    context.afterPropertiesSet()
    context
  }

  lazy val ldapAuthoritiesPopulator = {
    val populator =
      new DefaultLdapAuthoritiesPopulator(contextSource, configService.get(GROUP_SEARCH_BASE, ""))
    populator.setSearchSubtree(true)
    populator.setConvertToUpperCase(false)
    populator.setRolePrefix("")
    configService.get(GROUP_SEARCH_FILTER) foreach { s => populator.setGroupSearchFilter(s.toString) }
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
      configService.get(USER_SEARCH_FILTER) foreach { filter =>
        authenticator.setUserSearch(new FilterBasedLdapUserSearch(configService.get(USER_SEARCH_BASE, ""), filter.toString, contextSource))
      }
      authenticator.afterPropertiesSet()

      new LdapAuthenticationProvider(authenticator, fullAuthoritiesPopulator)
    }
  }

  @Bean def adAuthProviderFactory = new AuthProviderFactory {
    val mode = "ad"

    def create() =
      new ActiveDirectoryLdapAuthenticationProvider(configService.get(DOMAIN).map(_.toString).getOrElse(null), serverUrl)
  }

  @Bean def ldapUserService = catching(classOf[Exception]).opt {
    new LdapUserServiceImpl(configService, ldapTemplate, ldapAuthoritiesPopulator)
  }.getOrElse(new UserServiceStub)

  @Bean def ldapGroupService = catching(classOf[Exception]).opt {
    new LdapGroupServiceImpl(configService, ldapTemplate, ldapUserService.asInstanceOf[LdapUserService])
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
}
