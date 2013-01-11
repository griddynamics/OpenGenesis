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
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.matchers.ShouldMatchers

@ContextConfiguration(classes = Array(classOf[LdapPluginContext], classOf[MockContext]), loader = classOf[AnnotationConfigContextLoader])
@RunWith(classOf[SpringJUnit4ClassRunner])
class LdapUserServiceITCase extends ShouldMatchers {
  @Autowired
  var userService: LdapUserService = _

  @Test def testUserExist() {
    assert(userService.doesUserExist("developer1"))
    assert(!userService.doesUserExist("notfound"))
  }

  @Test def testExistBunchOfUsers() {
    assert(userService.doUsersExist(Seq("developer1", "developer2")))
    assert(!userService.doUsersExist(Seq("developer1", "notfound")))
  }

  @Test def testFindByUsername() {
    val userOpt = userService.findByUsername("developer1")
    userOpt should be ('defined)

    val user = userOpt.get
    user should have(
      'username ("developer1"),
      'password (None),
      'firstName ("John"),
      'lastName ("Doe"),
      'email ("developer1@example.com"),
      'jobTitle (None)
    )

    user.groups should be ('defined)

    val groups = user.groups.get

    groups should have size(1)
    groups.toSet should equal(Set("developers"))
  }

  @Test def testGetWithCredentials() {
    val userOpt = userService.getWithCredentials("developer2")
    userOpt should be ('defined)

    val user = userOpt.get
    user should have(
      'username ("developer2"),
      'password (Some("developer2pswd")),
      'firstName (null),
      'lastName ("Twain"),
      'email (null),
      'jobTitle (None)
    )

    user.groups should be ('defined)

    val groups = user.groups.get

    groups should have size(1)
    groups.toSet should equal(Set("developers"))
  }

  @Test def testPrefixSearch() {
    val users = userService.search("de*")
    users.map(_.username).toSet should equal(Set("developer1", "developer2"))
  }

  @Test def testSuffixSearch() {
    val users = userService.search("*per1")
    users.map(_.username).toSet should equal(Set("developer1"))
  }

  @Test def testSubStringSearch() {
    val users = userService.search("*er1*")
    users.map(_.username).toSet should equal(Set("developer1", "manager1"))
  }

  @Test def testList() {
    val users = userService.list
    users.map(_.username).toSet should equal(Set("developer1", "manager1", "developer2"))
  }

  @Test def testSearchSorting() {
    val users = userService.search("*")
    users.map(_.username) should equal(List("developer1", "developer2", "manager1"))
  }

  @Test def testListSorting() {
    val users = userService.list
    users.map(_.username) should equal(List("developer1", "developer2", "manager1"))
  }

  @Test def testGetUserGroups() {
    val groups = userService.getUserGroups("manager1")
    groups should be ('defined)
    groups.get.toSet should equal(Set("managers"))
  }

  @Test def testSearchPatternIsCaseInsensitive() {
    val users = userService.search("DeV*1")
    users.map(_.username).toSet should equal(Set("developer1"))
  }

  @Test def testSearchByFirstName() {
    val users = userService.search("Jo*")
    users.map(_.username).toSet should equal(Set("developer1"))
  }

  @Test def testSearchByLastName() {
    val users = userService.search("*oe")
    users.map(_.username).toSet should equal(Set("developer1"))
  }

  @Test def testSearchByFirstNameCaseInsensitive() {
    val users = userService.search("jO*")
    users.map(_.username).toSet should equal(Set("developer1"))
  }

  @Test def testSearchByLastNameCaseInsensitive() {
    val users = userService.search("*OE")
    users.map(_.username).toSet should equal(Set("developer1"))
  }
}


