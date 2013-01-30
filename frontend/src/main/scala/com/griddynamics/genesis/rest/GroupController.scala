/*
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

package com.griddynamics.genesis.rest

import annotations.LinkTarget
import links.CollectionWrapper._
import links.{WebPath, LinkBuilder}
import links.HrefBuilder._
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.groups.GroupService
import javax.servlet.http.HttpServletRequest
import GenesisRestController._
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import com.griddynamics.genesis.api.{ExtendedResult, UserGroup}
import com.griddynamics.genesis.spring.ApplicationContextAware
import javax.validation.Valid
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest/groups"))
class GroupController extends RestApiExceptionsHandler with ApplicationContextAware {

  @Autowired var groupService: GroupService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(method = Array(RequestMethod.GET), params = Array("available"))
  @ResponseBody
  def available() = !groupService.isReadOnly

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(request: HttpServletRequest) = {
    implicit val req = request
    val top = WebPath(request)
    def wrapGroup(group: UserGroup) = {
      wrap(group).withLinks(LinkBuilder(top / group.id.get.toString, LinkTarget.SELF, classOf[UserGroup], GET, PUT, DELETE)).filtered()
    }
    wrapCollection(groupService.list.map(wrapGroup(_))).withLinksToSelf(classOf[UserGroup], GET, POST).filtered()
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def get(@PathVariable(value = "id") id: Int, request: HttpServletRequest) = {
    val group = find(id)
    implicit val req = request
    wrap(group).withLinksToSelf(GET, PUT, DELETE).filtered()
  }

  private def find(id: Int) = groupService.get(id).getOrElse(throw new ResourceNotFoundException("Group [id = %d] was not found".format(id)))

  @RequestMapping(method = Array(RequestMethod.GET), params = Array("tag"))
  @ResponseBody
  def pick(@RequestParam("tag") search: String) = {
    groupService.search(search + "*").map(item => Map("key" -> item.name, "value" -> item.name))
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@Valid @RequestBody request: UserGroup) = groupService.create(request)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable(value = "id") id: Int, @Valid @RequestBody request: UserGroup) = {
    val group = find(id).copy(
      name = request.name,
      description = request.description,
      mailingList = request.mailingList,
      users = request.users
    )
    groupService.update(group)
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable(value = "id") id: Int) = groupService.delete(find(id))

  @RequestMapping(value = Array("{id}/users"), method = Array(RequestMethod.GET))
  @ResponseBody
  def listUsers(@PathVariable(value = "id") id: Int) = groupService.users(id)

  @RequestMapping(value = Array("{id}/users"), method = Array(RequestMethod.POST))
  @ResponseBody
  def addUser(@PathVariable(value = "id") id: Int, request: HttpServletRequest) = {
    RequestReader.read(request) { map =>
      val username = extractValue("username", map)
      withGroup(id) { _ =>
        groupService.users(id).filter(_.username == username).headOption match {
          case Some(s) => throw new ResourceConflictException
          case None => groupService.addUserToGroup(id, username)
        }
      }
    }
  }

  @RequestMapping(value = Array("{id}/users"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def removeUser(@PathVariable(value = "id") id: Int, request: HttpServletRequest) = RequestReader.read(request) {
    map => withGroup(id) { _ =>
      groupService.removeUserFromGroup(id, extractValue("username", map))
    }
  }

  def withGroup(id: Int)(block: UserGroup => ExtendedResult[_]) = block(find(id))
}
