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


class ExternalUserDetailsService(authorityService: AuthorityService, projectAuthorityService: ProjectAuthorityService, adminUsername: String)
  extends UserDetailsService with GroupBasedRoleService {

    // this is never called actually:
    def loadUserByUsername(username: String) = loadUserByUsername(username, Seq())

    def loadUserByUsername(username: String, groupNames : Iterable[String]) = {
        if (username != adminUsername) {
            val authorities = (
              authorityService.getUserAuthorities(username) ++
                (if (projectAuthorityService.isUserProjectAdmin(username, groupNames)) List(GenesisRole.ProjectAdmin.toString) else List())
              ).distinct
            new User(username, username, RoleBasedAuthority(authorities))
        } else {
            new User(adminUsername, "", Arrays.asList(RoleBasedAuthority(GenesisRole.GenesisUser), RoleBasedAuthority(GenesisRole.SystemAdmin)))
        }
    }

    def getRolesByGroupName(groupName: String): List[GrantedAuthority] = {
        new SimpleGrantedAuthority("GROUP_" + groupName) :: RoleBasedAuthority(authorityService.getGroupAuthorities(groupName)).toList
    }
}

trait GroupBasedRoleService extends UserDetailsService {
    def getRolesByGroupName(groupName: String) : List[GrantedAuthority]
}

class ExternalUserAuthenticationProvider(details: ExternalUserDetailsService) extends AuthenticationProvider with Logging {

    def authenticate(authentication: Authentication) = {
        val authRequest: ExternalAuthentication = authentication.asInstanceOf[ExternalAuthentication]
        val user = details.loadUserByUsername(authRequest.username, authRequest.assignedGroups)
        val additionalGroups = authRequest.assignedGroups.flatMap(details.getRolesByGroupName(_)).toList ++ user.getAuthorities
        if(additionalGroups.find(_.getAuthority == GenesisRole.GenesisUser.toString).isEmpty) {
            throw new UsernameNotFoundException("User %s doesn't have required role [%s]".format(authRequest.username, GenesisRole.GenesisUser))
        }
        new ExternalAuthentication(user, additionalGroups.toList)
    }

    def supports(authentication: Class[_]) = classOf[ExternalAuthentication].isAssignableFrom(authentication)
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
