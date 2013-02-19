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

package com.griddynamics.genesis.ad

import org.springframework.security.authentication.{BadCredentialsException, UsernamePasswordAuthenticationToken, AuthenticationProvider}
import org.springframework.security.core.Authentication
import com.griddynamics.genesis.util.Logging
import com4j.typelibs.ado20.Fields
import org.springframework.security.core.userdetails.{User, UsernameNotFoundException}
import com4j.typelibs.activeDirectory.{IADsGroup, IADsUser, IADsOpenDSObject}
import com4j.{ComException, COM4J}
import scala.collection.JavaConversions._
import java.util.Date
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}
import com.griddynamics.genesis.api.UserGroup
import com.griddynamics.genesis.users.GenesisRole
import com.griddynamics.genesis.spring.security.RoleBasedAuthority
import service.AbstractActiveDirectoryService

class ActiveDirectoryAuthenticationProvider(val namingContext: String,
                                            val pluginConfig: ActiveDirectoryPluginConfig,
                                            val projectAuthorityService: ProjectAuthorityService,
                                            val authorityService: AuthorityService,
                                            val template: CommandTemplate) extends AbstractActiveDirectoryService with AuthenticationProvider with Logging {

  case class UserQuery(username: String)
    extends Command(namingContext, "(&(sAMAccountName=%s)(sAMAccountType=805306368))".format(username), "distinguishedName,sAMAccountName", "subTree")

  case class UserIdentity(dn: String, domain: String, account: String)

  object UserMapper extends FieldsMapper[UserIdentity] with MappingUtils {
    protected def config = pluginConfig

    def mapFromFields(fields: Fields) = UserIdentity(
      getStringField("distinguishedName", fields)
        .getOrElse(throw new IllegalArgumentException("AD attribute 'distinguishedName' is empty")),
      getDomain(fields),
      getAccountName(fields)
    )
  }

  def authenticate(authentication: Authentication) = {
    if (!supports(authentication.getClass))
      throw new IllegalArgumentException("Invalid authentication argument: %s".format(authentication.getClass))

    val userToken = authentication.asInstanceOf[UsernamePasswordAuthenticationToken]

    val username = userToken.getName
    val password = Option(userToken.getCredentials.asInstanceOf[String]).getOrElse("")

    log.debug("Processing authentication request for user: %s", username)

    if (username == null || username == "")
      throw new BadCredentialsException("Username must be non-empty")

    //todo: handle empty password (empty password causes service error 0x80072020 during bind operation)

    val user = template.query(UserQuery(escape(username)), UserMapper).headOption getOrElse {
      throw new UsernameNotFoundException("User '%s' not found".format(username))
    }

    val principal = bind(user, password)

    log.debug("Authenticated Principal: " + principal)

    val result = new UsernamePasswordAuthenticationToken(principal, password, principal.getAuthorities)
    result.setDetails(userToken.getDetails)

    result
  }

  def supports(authentication: Class[_]) =
    classOf[UsernamePasswordAuthenticationToken].isAssignableFrom(authentication)

  private def bind(user: UserIdentity, password: String): User = {
    val dso: IADsOpenDSObject = COM4J.getObject(classOf[IADsOpenDSObject], "LDAP:", null)

    val retrievedUser = try {
      dso.openDSObject("LDAP://" + user.dn, user.dn, password, 0).queryInterface(classOf[IADsUser])
    } catch {
      case e: ComException if e.getHRESULT == ComErrors.AUTH_FAILED =>
        log.debug("Authentication failed for user: " + user.account); null
      case e: Exception =>
        log.warn(e, "Active Directory Interaction Error"); null
    }

    if (retrievedUser == null)
      throw new UsernameNotFoundException("User DN '%s' not found".format(user.dn))

    val groups = retrievedUser.groups().iterator().toList map { g =>
      val name = g.queryInterface(classOf[IADsGroup]).name().substring(3)
      if (pluginConfig.useDomain) user.domain + "\\" + name else name
    }

    log.debug("User '%s'; AD groups: %s", user.account, groups)

    new User(user.account, password, !isAccountDisabled(retrievedUser), !isAccountExpired(retrievedUser),
      true, !isAccountLocked(retrievedUser), populateAuthorities(user.account, groups))
  }

  private def populateAuthorities(username: String, groupNames: Seq[String]) = {
    val groups = groupNames.map(group => UserGroup(group, null, None, None, None))
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

  private def isAccountDisabled(user: IADsUser): Boolean = try {
    user.accountDisabled
  } catch {
    case e: ComException if e.getHRESULT == ComErrors.NON_CACHED_FIELD => false
  }

  private def isAccountLocked(user: IADsUser): Boolean = try {
    user.isAccountLocked
  } catch {
    case e: ComException if e.getHRESULT == ComErrors.NON_CACHED_FIELD => false
  }

  private def isAccountExpired(user: IADsUser): Boolean = try {
    Option(user.accountExpirationDate()) map { _.before(new Date()) } getOrElse (false)
  } catch {
    case e: ComException if e.getHRESULT == ComErrors.NON_CACHED_FIELD => false
  }

}
