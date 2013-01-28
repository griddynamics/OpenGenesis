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
import org.springframework.ldap.core.{ContextMapperCallbackHandler, DirContextAdapter, ContextMapper, LdapTemplate}
import com.griddynamics.genesis.api.UserGroup
import util.control.Exception._
import scala.collection.JavaConversions._
import org.springframework.dao.IncorrectResultSizeDataAccessException
import javax.naming.ldap.LdapName
import com.griddynamics.genesis.util.Logging
import javax.naming.directory.SearchControls
import org.springframework.ldap.{TimeLimitExceededException, SizeLimitExceededException}

trait LdapGroupService extends GroupService

class LdapGroupServiceImpl(val config: LdapPluginConfig,
                           template: => LdapTemplate, // by-name parameters are needed to avoid failure at startup
                           userService: => LdapUserService) extends LdapGroupService with Logging {

  override def isReadOnly = true

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

  private def findGroup(filter: String): Option[UserGroup] = {
    log.debug("Group search base: '%s'; filter: '%s'", config.groupSearchBase, filter)

    catching(classOf[IncorrectResultSizeDataAccessException]).opt(
      template.searchForObject(
        config.groupSearchBase,
        filter,
        GroupContextMapper(includeUsers = true)
      ).asInstanceOf[UserGroup]
    )
  }

  def findByName(name: String) = findGroup(filterByNamePattern(config.stripDomain(name)))

  def findByNames(names: Iterable[String]) = {
    val filter = "(&(%s)(|%s))".format(
      config.groupsServiceFilter,
      names.map { name => "(cn=%s)".format(config.stripDomain(name)) }.mkString
    )

    log.debug("Group search base: '%s'; filter: '%s'", config.groupSearchBase, filter)

    search(filter, GroupContextMapper()).toSet
  }

  def users(id: Int) = get(id) match {
    case Some(group) => group.users.map { _.flatMap(userService.findByUsername(_)) }.getOrElse(Seq.empty)
    case _ => Seq.empty
  }

  def addUserToGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def removeUserFromGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def get(id: Int) = findGroup(filterById(id.toString))

  def getUsersGroups(username: String) = userService.getUserGroups(config.stripDomain(username)) match {
    case Some(groups) => groups flatMap { findByName(_) }
    case _ => Seq.empty
  }

  def setUsersGroups(username: String, groups: Seq[String]) {
    throw new UnsupportedOperationException
  }

  def search(nameLike: String) = search(filterByNamePattern(nameLike), GroupContextMapper())

  def doesGroupExist(groupName: String) = findByName(config.stripDomain(groupName)).isDefined

  def doGroupsExist(groupNames: Iterable[String]) =
    findByNames(groupNames).map(_.name.toLowerCase).toSet == groupNames.map(_.toLowerCase).toSet

  def list = search(config.groupsServiceFilter, GroupContextMapper())

  private def search(filter: String, mapper: ContextMapper) = {
    val handler = new ContextMapperCallbackHandler(mapper)
    val controls =
      new SearchControls(SearchControls.SUBTREE_SCOPE, config.sizeLimit, config.timeout, null, true, false)

    log.debug("Group search base: '%s'; filter: '%s'", config.groupSearchBase, filter)

    try {
      template.search(config.groupSearchBase, filter, controls, handler)
    } catch {
      case e: SizeLimitExceededException =>
        log.warn("Size limit exceeded error occured. Found %s records", handler.getList.size())
      case e: TimeLimitExceededException =>
        log.warn("Time limit exceeded error occured. Found %s records", handler.getList.size())
    }

    handler.getList.toList.asInstanceOf[List[UserGroup]].sortBy(_.name.toLowerCase)
  }

}
