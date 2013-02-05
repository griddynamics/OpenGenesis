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

package com.griddynamics.genesis.ad.service

import com.griddynamics.genesis.groups.GroupService
import com.griddynamics.genesis.util.Logging
import com4j.typelibs.ado20.Fields
import com.griddynamics.genesis.ad._
import com.griddynamics.genesis.api.UserGroup
import com.griddynamics.genesis.annotation.RemoteGateway

trait ActiveDirectoryGroupService extends GroupService

@RemoteGateway("Acrive directory access: group service")
class ActiveDirectoryGroupServiceImpl(val namingContext: String,
                                      val pluginConfig: ActiveDirectoryPluginConfig,
                                      val template: CommandTemplate) extends AbstractActiveDirectoryService with ActiveDirectoryGroupService with Logging {

  override def isReadOnly = true

  object GroupMapper extends FieldsMapper[UserGroup] with MappingUtils {
    protected def config = pluginConfig

    def mapFromFields(fields: Fields) =
      UserGroup(
        getAccountName(fields),
        getStringField("description", fields).getOrElse(""),
        getStringField("mail", fields),
        None,
        None
      )
  }

  case class Query(override val filter: String)
    extends Command(namingContext, "(&(%s)(sAMAccountType=268435456))".format(filter), "distinguishedName,sAMAccountName,description,mail", "subTree")

  def findByName(name: String) =
    template.query(Query("(sAMAccountName=%s)".format(normalize(name))), GroupMapper).headOption

  def findByNames(names: Iterable[String]): Set[UserGroup] = {
    if (names.isEmpty)
      return Set()

    val filter = "(|%s)".format(
      names.map { name => "(sAMAccountName=%s)".format(normalize(name)) }.mkString
    )
    template.query(Query(filter), GroupMapper).toSet
  }

  def users(name: Int) = throw new UnsupportedOperationException

  def addUserToGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def removeUserFromGroup(id: Int, username: String) = throw new UnsupportedOperationException

  def get(id: Int) = throw new UnsupportedOperationException

  def getUsersGroups(username: String) = throw new UnsupportedOperationException

  def setUsersGroups(username: String, groups: Seq[String]) {
    throw new UnsupportedOperationException
  }

  def search(nameLike: String) =
    template.query(Query("(sAMAccountName=%s)".format(escape(nameLike))), GroupMapper).toList

  def doesGroupExist(groupName: String) = findByName(groupName).isDefined

  def doGroupsExist(groupNames: Iterable[String]) = groupNames forall { g => doesGroupExist(g) }

  def list = template.query(Query("(*)"), GroupMapper)
}
