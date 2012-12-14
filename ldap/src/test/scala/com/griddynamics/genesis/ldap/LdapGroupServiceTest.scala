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
import com.griddynamics.genesis.service.ConfigService
import org.springframework.ldap.core.{ContextMapper, LdapTemplate}
import org.mockito.Matchers
import com.griddynamics.genesis.api.{UserGroup, User}
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

class LdapGroupServiceTest extends ShouldMatchers {

  var ldapTemplate: LdapTemplate = _
  var groupService: LdapGroupService = _

  @Before
  def setUp() {
    val config = mock(classOf[LdapPluginConfig])
    val userService = mock(classOf[LdapUserService])
    ldapTemplate = mock(classOf[LdapTemplate])

    groupService = new LdapGroupServiceImpl(config, ldapTemplate, userService)
  }

  @Test def testListSorting() {
    when(ldapTemplate.search(
      Matchers.any(classOf[String]),
      Matchers.any(classOf[String]),
      Matchers.any(classOf[ContextMapper])))
      .thenAnswer(new Answer[java.util.List[_]] {
      def answer(invocation: InvocationOnMock) = {
        java.util.Arrays.asList(
          UserGroup("wyzwa", null, None, None, None),
          UserGroup("_groupname", null, None, None, None),
          UserGroup("developer", null, None, None, None),
          UserGroup("DeVelopers", null, None, None, None),
          UserGroup("__groupname", null, None, None, None),
          UserGroup("WYZame", null, None, None, None),
          UserGroup("WYZW", null, None, None, None)
        )
      }
    })

    val users = groupService.list
    users.map(_.name) should equal(List("__groupname", "_groupname", "developer", "DeVelopers", "WYZame", "WYZW", "wyzwa"))
  }

  @Test def testSearchSorting() {
    when(ldapTemplate.search(
      Matchers.any(classOf[String]),
      Matchers.any(classOf[String]),
      Matchers.any(classOf[ContextMapper])))
      .thenAnswer(new Answer[java.util.List[_]] {
      def answer(invocation: InvocationOnMock) = {
        java.util.Arrays.asList(
          UserGroup("wyszwa", null, None, None, None),
          UserGroup("_groupsname", null, None, None, None),
          UserGroup("develosper", null, None, None, None),
          UserGroup("DeVelospers", null, None, None, None),
          UserGroup("__groupsname", null, None, None, None),
          UserGroup("WYZasme", null, None, None, None),
          UserGroup("WYsZW", null, None, None, None)
        )
      }
    })
    val users = groupService.search("*s*")
    users.map(_.name) should equal(List("__groupsname", "_groupsname", "develosper", "DeVelospers", "WYsZW", "wyszwa", "WYZasme"))
  }

}
