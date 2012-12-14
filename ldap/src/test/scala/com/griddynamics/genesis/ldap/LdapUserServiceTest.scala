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

import org.junit.{Before, Test}
import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito._
import org.springframework.ldap.core.{ContextMapper, LdapTemplate}
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import org.mockito.Matchers
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import com.griddynamics.genesis.api.User

class LdapUserServiceTest extends ShouldMatchers {

  var ldapTemplate: LdapTemplate = _
  var userService: LdapUserService = _

  @Before
  def setUp() {
    val config = mock(classOf[LdapPluginConfig])
    when(config.userSearchFilter).thenReturn("uid={0}")

    val authoritiesPopulator = mock(classOf[LdapAuthoritiesPopulator])
    ldapTemplate = mock(classOf[LdapTemplate])

    userService = new LdapUserServiceImpl(config, ldapTemplate, authoritiesPopulator)
  }

  @Test def testListSorting() {
    when(ldapTemplate.search(
      Matchers.any(classOf[String]),
      Matchers.any(classOf[String]),
      Matchers.any(classOf[ContextMapper])))
      .thenAnswer(new Answer[java.util.List[_]] {
      def answer(invocation: InvocationOnMock) = {
        java.util.Arrays.asList(
          User("wyzwa", null, null, null, None, None, None),
          User("_username", null, null, null, None, None, None),
          User("azhuchkoff", null, null, null, None, None, None),
          User("AZhuchkov", null, null, null, None, None, None),
          User("__username", null, null, null, None, None, None),
          User("WYZame", null, null, null, None, None, None),
          User("WYZW", null, null, null, None, None, None)
        )
      }
    })

    val users = userService.list
    users.map(_.username) should equal(List("__username", "_username", "azhuchkoff", "AZhuchkov", "WYZame", "WYZW", "wyzwa"))
  }

  @Test def testSearchSorting() {
    when(ldapTemplate.search(
      Matchers.any(classOf[String]),
      Matchers.any(classOf[String]),
      Matchers.any(classOf[ContextMapper])))
      .thenAnswer(new Answer[java.util.List[_]] {
      def answer(invocation: InvocationOnMock) = {
        java.util.Arrays.asList(
          User("wyszwa", null, null, null, None, None, None),
          User("_usersname", null, null, null, None, None, None),
          User("azhuchskoff", null, null, null, None, None, None),
          User("AZhuchskov", null, null, null, None, None, None),
          User("__usersname", null, null, null, None, None, None),
          User("WYZasme", null, null, null, None, None, None),
          User("WYsZW", null, null, null, None, None, None)
        )
      }
    })
    val users = userService.search("*s*")
    users.map(_.username) should equal(List("__usersname", "_usersname", "azhuchskoff", "AZhuchskov", "WYsZW", "wyszwa", "WYZasme"))
  }

}
