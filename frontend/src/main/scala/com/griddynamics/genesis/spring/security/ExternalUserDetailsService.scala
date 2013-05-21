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

package com.griddynamics.genesis.spring.security

import com.griddynamics.genesis.service.{AuthorityService, ProjectAuthorityService}
import org.springframework.security.core.userdetails.{UserDetails, User, UsernameNotFoundException, UserDetailsService}
import com.griddynamics.genesis.users.GenesisRole
import java.util.Arrays
import scala.collection.JavaConversions._
import org.springframework.security.authentication.{AbstractAuthenticationToken, AuthenticationProvider}
import org.springframework.security.core.{GrantedAuthority, Authentication}
import com.griddynamics.genesis.util.Logging
import org.springframework.security.core.authority.SimpleGrantedAuthority
import GenesisRole._

trait GroupBasedRoleService extends UserDetailsService {
  def getRolesByGroupNames(groupNames: String*): Iterable[GrantedAuthority]
}

class ExternalUserDetailsService(authorityService: AuthorityService, projectAuthorityService: ProjectAuthorityService, adminUsername: String)
  extends UserDetailsService with GroupBasedRoleService {

    // this is never called actually:
    def loadUserByUsername(username: String) = loadUserByUsername(username, Seq())

    def loadUserByUsername(username: String, groupNames : Iterable[String]) = {
        if (username.toUpperCase != adminUsername.toUpperCase) {
            var authorities = (
              authorityService.getUserAuthorities(username) ++
                (if (projectAuthorityService.isUserProjectAdmin(username, groupNames)) List(ProjectAdmin.toString) else List())
              )
            if (authorities.contains(SystemAdmin.toString) || authorities.contains(ReadonlySystemAdmin.toString)) {
              authorities = GenesisUser.toString :: authorities
            }
            new User(username, username, RoleBasedAuthority(authorities.distinct))
        } else {
            new User(adminUsername, "", Arrays.asList(RoleBasedAuthority(GenesisUser), RoleBasedAuthority(SystemAdmin)))
        }
    }

    def getRolesByGroupNames(groupNames: String*) = groupNames.map(gn =>
      new SimpleGrantedAuthority(s"GROUP_$gn")
    ) ++ RoleBasedAuthority(authorityService.getGroupsAuthorities(groupNames :_*))

}

class ExternalUserAuthenticationProvider(details: ExternalUserDetailsService) extends AuthenticationProvider with Logging {

  private val ALLOWED_ROLES = Seq(GenesisUser, SystemAdmin, ReadonlySystemAdmin).map(_.toString)

  def authenticate(authentication: Authentication) = {
    val authRequest: ExternalAuthentication = authentication.asInstanceOf[ExternalAuthentication]
    val assignedGroups = authRequest.assignedGroups
    val username = authRequest.username
    val user = details.loadUserByUsername(username, assignedGroups)
    val additionalGroups = getAdditionalGroups(user, assignedGroups :_*)
    if(!additionalGroups.exists( gr => ALLOWED_ROLES.contains(gr.getAuthority)))
      throw new UsernameNotFoundException(s"User $username doesn't have required role [$GenesisUser]")
    new ExternalAuthentication(user, additionalGroups.toList)
  }

  def supports(authentication: Class[_]) = classOf[ExternalAuthentication].isAssignableFrom(authentication)

  private def getAdditionalGroups(user: User, assignedGroups: String*) = {
    lazy val groupRoles = details.getRolesByGroupNames(assignedGroups: _*)
    // in case of user is system admin there's no need of any more roles(avoid loading roles from DB)
    (if(isSysAdmin(user)) Seq() else groupRoles) ++ user.getAuthorities
  }

  private def isSysAdmin(user: User) = user.getAuthorities.exists(_.getAuthority == SystemAdmin.toString)
}

class ExternalAuthentication(val username: String, val assignedGroups: List[String] = List(),
                             authorities: List[GrantedAuthority] = List(), userDetails: UserDetails = null) extends AbstractAuthenticationToken(authorities) {
    var principal: AnyRef = username

    def this(details: UserDetails, additionalGroups: List[GrantedAuthority]) = {
        this(null, null, additionalGroups.distinct, details)
        principal = details
        setAuthenticated(true)
    }

    def getCredentials = ""
    def getPrincipal = principal
}
