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
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.AnnotationConfigContextLoader
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.matchers.ShouldMatchers
import com.griddynamics.genesis.groups.GroupService
import org.junit.Test

@ContextConfiguration(classes = Array(classOf[LdapPluginContext], classOf[MockContext]), loader = classOf[AnnotationConfigContextLoader])
@RunWith(classOf[SpringJUnit4ClassRunner])
class LdapGroupServiceITCase extends ShouldMatchers {
  @Autowired
  var groupService: GroupService = _

  @Test def testDoesGroupExist() {
    assert(groupService.doesGroupExist("developers"))
    assert(!groupService.doesGroupExist("notfound"))
  }

  @Test def testExistBunchOfGroups() {
    assert(groupService.doGroupsExist(Seq("developers", "managers")))
    assert(!groupService.doGroupsExist(Seq("notfound", "managers")))
  }

  @Test def testFindByName() {
    val groupOpt = groupService.findByName("developers")
    groupOpt should be ('defined)

    val group = groupOpt.get
    group should have(
      'id (None),
      'name ("developers"),
      'description ("Company Developers"),
      'mailingList (None)
    )

    group.users should be ('defined)

    group.users.get.toSet should be (Set("developer1", "developer2"))
  }

  @Test def testGet() {
    val groupOpt = groupService.get("managers")
    groupOpt should be ('defined)

    val group = groupOpt.get
    group should have(
      'id (None),
      'name ("managers"),
      'description (null),
      'mailingList (None)
    )

    group.users should be ('defined)

    group.users.get.toSet should be (Set("manager1"))
  }

  @Test def testGetUsersGroup() {
    groupService.getUsersGroups("developer1").map(_.name).toSet should equal(Set("developers"))
    groupService.getUsersGroups("developer2").map(_.name).toSet should equal(Set("developers"))
    groupService.getUsersGroups("manager1").map(_.name).toSet should equal(Set("managers"))
  }

  @Test def testPrefixSearch() {
    val groups = groupService.search("de*")
    groups.map(_.name).toSet should equal(Set("developers"))
  }

  @Test def testSuffixSearch() {
    val groups = groupService.search("*ers")
    groups.map(_.name).toSet should equal(Set("managers", "developers"))
  }

  @Test def testSubStringSearch() {
    val groups = groupService.search("*er*")
    groups.map(_.name).toSet should equal(Set("managers", "developers"))
  }

  @Test def testList() {
    val groups = groupService.list
    groups.map(_.name).toSet should equal(Set("managers", "developers"))
  }

  @Test def testSearchSorting() {
    val groups = groupService.search("*")
    groups.map(_.name) should equal(List("developers", "managers"))
  }

  @Test def testListSorting() {
    val groups = groupService.list
    groups.map(_.name) should equal(List("developers", "managers"))
  }

  @Test def testSearchPatternIsCaseInsensitive() {
    val group = groupService.search("DeV*")
    group.map(_.name).toSet should equal(Set("developers"))
  }

}


