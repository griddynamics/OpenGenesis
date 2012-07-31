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

import java.util.Arrays
import scala.collection.JavaConversions._
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.{UsernameNotFoundException, User, UserDetailsService}
import com.griddynamics.genesis.spring.ApplicationContextAware
import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.users.{GenesisRole, UserService}
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService}


class GenesisUserDetailsService( adminUsername: String,
                                 adminPassword: String,
                                 authorityService: AuthorityService,
                                 projectAuthorityService: ProjectAuthorityService
                               ) extends UserDetailsService with ApplicationContextAware {

  lazy val userService: Option[UserService] = Option(applicationContext.getBean(classOf[UserService]))
  lazy val groupService: Option[GroupService] = Option(applicationContext.getBean(classOf[GroupService]))

  def loadUserByUsername(username: String) = {
    if (username != adminUsername) {

      val user = for {
        service <- userService
        user <- service.getWithCredentials(username)
      } yield {
        val groups = groupService match {
          case None => List()
          case Some(gservice) => gservice.getUsersGroups(user.username)
        }
        val authorities = (
          authorityService.getAuthorities(groups) ++
            authorityService.getUserAuthorities(user.username) ++
            groups.map("GROUP_" + _.name) ++
            (if (projectAuthorityService.isUserProjectAdmin(user.username, groups.map(_.name))) List(GenesisRole.ProjectAdmin.toString) else List())
          ).distinct
        if(!authorities.contains(GenesisRole.GenesisUser.toString)) {
          throw new UsernameNotFoundException("User doesn't have required role [%s]".format(GenesisRole.GenesisUser))
        }
        new User(user.username, user.password.get, RoleBasedAuthority(authorities))
      }

      user.getOrElse(throw new UsernameNotFoundException("Couldn't find user = [" + username + "]"))

    } else {
      new User(adminUsername, adminPassword, Arrays.asList(RoleBasedAuthority(GenesisRole.GenesisUser), RoleBasedAuthority(GenesisRole.SystemAdmin)))
    }
  }
}

object RoleBasedAuthority {
  def apply(role: String): GrantedAuthority = new GrantedAuthority() {
    def getAuthority = role
  }

  def apply(roles: Iterable[String]): Iterable[GrantedAuthority] = roles.map (apply _)

  def apply(role: GenesisRole.Value): GrantedAuthority = new GrantedAuthority() {
    def getAuthority = role.toString
  }
}
