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

import com.griddynamics.genesis.groups.GroupService
import org.springframework.ldap.core.{DirContextAdapter, ContextMapper, LdapTemplate}
import com.griddynamics.genesis.api.UserGroup
import util.control.Exception._
import scala.collection.JavaConversions._
import org.springframework.dao.IncorrectResultSizeDataAccessException
import javax.naming.ldap.LdapName
import com.griddynamics.genesis.cache.{CacheManager, Cache}

trait LdapGroupService extends GroupService

object LdapGroupService {
  val SearchCacheRegion = "ldap-group-service-search"
}

class LdapGroupServiceImpl(val config: LdapPluginConfig,
                           val template: LdapTemplate,
                           val userService: LdapUserService,
                           val cacheManager: CacheManager) extends LdapGroupService with Cache {

  override def isReadOnly = true

  override def defaultTtl = config.cacheTtl
  override def maxEntries = config.cacheMaxEntries

  case class GroupContextMapper(includeUsers: Boolean = false) extends ContextMapper {
    def mapFromContext(ctx: Any): UserGroup = {
      val adapter = ctx.asInstanceOf[DirContextAdapter]

      val idOpt = Option(adapter.getStringAttribute("gidNumber")).map{ _.toInt }

      val usersOpt =
        if (includeUsers) {
          Option(adapter.getStringAttributes(config.groupMemberAttributeName)) map { _.toSeq map { dn =>
            val ldapName = new LdapName(dn)
            config.addDomain(ldapName.getRdn(ldapName.size() - 1).getValue.toString)
          }}
        } else None

      UserGroup(
        Option(config.addDomain(adapter.getStringAttribute("cn"))).getOrElse("UNKNOWN_NAME"),
        adapter.getStringAttribute("description"),
        None,
        idOpt,
        usersOpt
      )
    }
  }

  private def filter(attributeName: String, attributePattern: String) =
    "(&(%s)(%s=%s))".format(config.groupsServiceFilter, attributeName, attributePattern)

  private def filterByNamePattern(groupPattern: String) = filter("cn", groupPattern)

  private def filterById(id: String) = filter("gidNumber", id)

  def findByName(name: String) =
    catching(classOf[IncorrectResultSizeDataAccessException]).opt(
      template.searchForObject(
        config.groupSearchBase,
        filterByNamePattern(config.stripDomain(name)),
        GroupContextMapper(includeUsers = true)
      ).asInstanceOf[UserGroup]
    )

  def users(id: Int) = get(id) match {
    case Some(group) => group.users.map { _.flatMap(userService.findByUsername(_)) }.getOrElse(Seq.empty)
    case _ => Seq.empty
  }

  def addUserToGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def removeUserFromGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def get(id: Int) =
    catching(classOf[IncorrectResultSizeDataAccessException]).opt(
      template.searchForObject(
        config.groupSearchBase,
        filterById(id.toString),
        GroupContextMapper(includeUsers = true)
      ).asInstanceOf[UserGroup]
    )

  def getUsersGroups(username: String) = userService.getUserGroups(config.stripDomain(username)) match {
    case Some(groups) => groups flatMap { findByName(_) }
    case _ => Seq.empty
  }

  def setUsersGroups(username: String, groups: Seq[String]) {
    throw new UnsupportedOperationException
  }

  def search(nameLike: String) = fromCache(LdapGroupService.SearchCacheRegion, nameLike) {
    template.search(
      config.groupSearchBase,
      filterByNamePattern(nameLike),
      GroupContextMapper()
    ).toList.asInstanceOf[List[UserGroup]].sortBy(_.name.toLowerCase)
  }

  def doesGroupExist(groupName: String) = findByName(config.stripDomain(groupName)).isDefined

  def doGroupsExist(groupNames: Seq[String]) = groupNames forall { g => doesGroupExist(config.stripDomain(g)) }

  def list = template.search(
    config.groupSearchBase,
    config.groupsServiceFilter,
    GroupContextMapper()
  ).toList.asInstanceOf[List[UserGroup]].sortBy(_.name.toLowerCase)

}
