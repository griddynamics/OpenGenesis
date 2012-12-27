/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.ldap

import com.griddynamics.genesis.cache.{CacheConfig, CacheManager}
import org.springframework.ldap.core.{ContextMapper, LdapTemplate}
import org.junit.{Before, Test}
import org.mockito.Mockito._
import com.griddynamics.genesis.api.User
import scala.Some
import org.mockito.Matchers
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.scalatest.matchers.ShouldMatchers

class TestService(val cacheManager: CacheManager, val template: LdapTemplate) extends WildcardCaching {
  import scala.collection.JavaConversions._

  def defaultTtl = 30
  def maxEntries = 100

  def search(usernameLike: String): List[User] = {
    def cacheFilter(user: User): Boolean = {
      val wildcard = Wildcard(usernameLike)
      wildcard.accept(user.username) || wildcard.accept(user.firstName) || wildcard.accept(user.lastName)
    }

    fromCache(LdapUserService.SearchCacheRegion, usernameLike, cacheFilter) {
      template.search(
        "",
        "uid=" + usernameLike,
        DummyContextMapper
      ).toList.asInstanceOf[List[User]].sortBy(_.username.toLowerCase)
    }
  }

  object DummyContextMapper extends ContextMapper {
    def mapFromContext(ctx: Any) = null
  }
}

class WildcardCachingTest extends ShouldMatchers {

  var ldapTemplate: LdapTemplate = _
  var cacheManager: CacheManager = _
  var userService: TestService = _

  @Before
  def setUp() {
    ldapTemplate = mock(classOf[LdapTemplate])
    cacheManager = mock(classOf[CacheManager])

    userService = new TestService(cacheManager, ldapTemplate)
  }

  @Test def testSearchCacheHit() {
    when(cacheManager.fromCache(
      Matchers.eq(LdapUserService.SearchCacheRegion),
      Matchers.any(classOf[(String) => Boolean]))
    ).thenReturn(Some(List(
      User("operator", null, "Operator", null, None, None, None),
      User("developer1", null, null, null, None, None, None),
      User("develOper2", null, null, null, None, None, None),
      User("manager1", null, "DevelOper", null, None, None, None),
      User("manager2", null, null, "Lop", None, None, None)
    )))

    val users = userService.search("*lop*")

    verify(cacheManager).createCacheIfAbsent(Matchers.any(classOf[CacheConfig]))

    verify(cacheManager)
      .fromCache(Matchers.eq(LdapUserService.SearchCacheRegion), Matchers.any(classOf[(String) => Boolean]))

    verifyNoMoreInteractions(cacheManager)
    verifyNoMoreInteractions(ldapTemplate)

    users.map(_.username).toSet should equal(Set("developer1", "develOper2", "manager1", "manager2"))
  }

  @Test def testSearchCacheMiss() {
    when(cacheManager.fromCache(
      Matchers.eq(LdapUserService.SearchCacheRegion),
      Matchers.any(classOf[(String) => Boolean]))
    ).thenReturn(None)

    when(ldapTemplate.search(
      Matchers.any(classOf[String]),
      Matchers.any(classOf[String]),
      Matchers.any(classOf[ContextMapper]))
    ).thenAnswer(new Answer[java.util.List[_]] {
      def answer(invocation: InvocationOnMock) = {
        java.util.Arrays.asList(
          User("developer1", null, null, null, None, None, None),
          User("deveLoper2", null, null, null, None, None, None),
          User("user1", null, "DevelOper", null, None, None, None),
          User("user2", null, null, "Lop", None, None, None),
          User("user3", null, null, null, None, None, None) // wrong record (should not be filtered)
        )
      }
    })

    val searchPattern: String = "*lop*"
    val users = userService.search(searchPattern)

    verify(ldapTemplate).search(Matchers.any(classOf[String]),
      Matchers.any(classOf[String]), Matchers.any(classOf[ContextMapper]))

    verifyNoMoreInteractions(ldapTemplate)

    verify(cacheManager).createCacheIfAbsent(Matchers.any(classOf[CacheConfig]))

    verify(cacheManager)
      .fromCache(Matchers.eq(LdapUserService.SearchCacheRegion), Matchers.any(classOf[(String) => Boolean]))

    verify(cacheManager).putInCache(Matchers.eq(LdapUserService.SearchCacheRegion),
      Matchers.eq(searchPattern), Matchers.any(classOf[List[User]]))

    verifyNoMoreInteractions(cacheManager)

    users.map(_.username).toSet should equal(Set("developer1", "deveLoper2", "user1", "user2", "user3"))
  }
}
