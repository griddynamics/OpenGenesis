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

package com.griddynamics.genesis.rest

import annotations.LinkTarget
import links.CollectionWrapper._
import links.{WebPath, LinkBuilder}
import links.HrefBuilder._
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.groups.GroupService
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import com.griddynamics.genesis.api.User
import javax.validation.Valid
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping(Array("/rest/users"))
class UsersController extends RestApiExceptionsHandler {

  @Autowired var userService: UserService = _
  @Autowired var groupService: GroupService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(method = Array(RequestMethod.GET), params = Array("available"))
  @ResponseBody
  def available() = !userService.isReadOnly

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def list(request: HttpServletRequest) = {
    implicit val req = request
    val top = WebPath(request)
    def wrapUser(user: User) = {
      wrap(user).withLinks(LinkBuilder(top / user.username, LinkTarget.SELF, classOf[User], GET, PUT, DELETE)).filtered()
    }
    wrapCollection(userService.list.map(wrapUser(_))).withLinksToSelf(classOf[User], GET, POST).filtered()
  }

  @RequestMapping(method = Array(RequestMethod.GET), params = Array("tag"))
  @ResponseBody
  def pick(@RequestParam("tag") search: String) = {

    def formatLabel(user: User): String = {
      val nonEmpty = (str: String) => { Option(str).getOrElse("").nonEmpty }

      val buffer = new StringBuilder
      if (nonEmpty(user.firstName)) buffer.append(user.firstName).append(" ")
      if (nonEmpty(user.lastName)) buffer.append(user.lastName).append(" ")

      val nameExists = buffer.nonEmpty
      if (nameExists) buffer.append("(")
      buffer.append(user.username)
      if (nameExists) buffer.append(")")

      buffer.toString()
    }

    userService.search(search + "*").map(item =>
      Map(
        "key" -> item.username,
        "value" -> formatLabel(item)
      )
    )
  }

  @RequestMapping(value = Array("{username:.+}"), method=Array(RequestMethod.GET))
  @ResponseBody
  def get(@PathVariable(value = "username") username: String, request: HttpServletRequest) = {
    implicit val req = request
    val user = find(username)
    wrap(user).withLinksToSelf(GET, PUT, DELETE).filtered()
  }

  private def find(username: String) = userService.findByUsername(username).getOrElse(throw new ResourceNotFoundException("User[username = " + username + "] was not found"))

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@RequestBody @Valid request: User) = userService.create(request)

  @RequestMapping(value = Array("{username:.+}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable("username") username: String, @RequestBody @Valid request: User) = {
    val user = find(username).copy(
      email = request.email,
      firstName = request.firstName,
      lastName = request.lastName,
      jobTitle = request.jobTitle,
      groups = request.groups,
      password = None
    )
    userService.update(user)
  }

  @RequestMapping(value = Array("{username:.+}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable(value="username") username: String) = userService.delete(find(username))

  @RequestMapping(value = Array("{userName}/groups"), method = Array(RequestMethod.GET))
  @ResponseBody
  def userGroups(@PathVariable("userName") userName: String) = groupService.getUsersGroups(userName)
}
