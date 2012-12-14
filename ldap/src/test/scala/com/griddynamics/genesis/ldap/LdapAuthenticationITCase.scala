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

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.junit.Test
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import com.griddynamics.genesis.spring.security.{ExternalAuthentication, AuthProviderFactory}
import org.springframework.security.authentication.{BadCredentialsException, UsernamePasswordAuthenticationToken}
import org.springframework.security.core.{GrantedAuthority, Authentication}
import scala.collection.JavaConversions._
import com.griddynamics.genesis.users.GenesisRole
import org.scalatest.matchers.ShouldMatchers

@ContextConfiguration(classes = Array(classOf[LdapPluginContext], classOf[MockContext]), loader = classOf[AnnotationConfigContextLoader])
@RunWith(classOf[SpringJUnit4ClassRunner])
class LdapAuthenticationITCase extends ShouldMatchers {
  @Autowired
  @Qualifier("ldapAuthProviderFactory")
  var ldapAuthProviderFactory: AuthProviderFactory = _

  @Test
  def testAuthProviderIsCapable() {
    ldapAuthProviderFactory should be ('capable)
  }

  @Test
  def testSuccessfulAuthentication() {
    val token = new UsernamePasswordAuthenticationToken("developer1", "developer1pswd")
    val authentication = ldapAuthProviderFactory.create().authenticate(token)

    authentication should be ('isAuthenticated)
  }

  @Test
  def testAdministratorAuthorities() {
    val token = new UsernamePasswordAuthenticationToken("developer1", "developer1pswd")
    val authentication = ldapAuthProviderFactory.create().authenticate(token)

    val expectedAuthorities = Set("GROUP_developers", GenesisRole.GenesisUser.toString, GenesisRole.SystemAdmin.toString)
    val actualAuthorities = authentication.getAuthorities.toSet.map{ e: GrantedAuthority => e.getAuthority }

    actualAuthorities should equal(expectedAuthorities)
  }

  @Test
  def testNonAdministratorAuthorities() {
    val token = new UsernamePasswordAuthenticationToken("developer2", "developer2pswd")
    val authentication = ldapAuthProviderFactory.create().authenticate(token)

    val expectedAuthorities = Set("GROUP_developers")
    val actualAuthorities = authentication.getAuthorities.toSet.map{ e: GrantedAuthority => e.getAuthority }

    actualAuthorities should equal(expectedAuthorities)
  }

  @Test
  def testGroupAuthorities() {
    val token = new UsernamePasswordAuthenticationToken("manager1", "manager1pswd")
    val authentication = ldapAuthProviderFactory.create().authenticate(token)

    val expectedAuthorities =
      Set("GROUP_managers", GenesisRole.GenesisUser.toString, GenesisRole.ReadonlySystemAdmin.toString)

    val actualAuthorities = authentication.getAuthorities.toSet.map{ e: GrantedAuthority => e.getAuthority }

    actualAuthorities should equal(expectedAuthorities)
  }

  @Test(expected = classOf[BadCredentialsException])
  def testAuthenticationWithWrongCredentials() {
    val token = new UsernamePasswordAuthenticationToken("developer1", "pswd")
    ldapAuthProviderFactory.create().authenticate(token)
  }

  @Test(expected = classOf[BadCredentialsException])
  def testAuthenticationWithWrongUsername() {
    val token = new UsernamePasswordAuthenticationToken("hacker", "pswd")
    ldapAuthProviderFactory.create().authenticate(token)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testWrongAuthenticationObject() {
    val token: Authentication = new ExternalAuthentication("developer1")
    ldapAuthProviderFactory.create().authenticate(token)
  }

}


