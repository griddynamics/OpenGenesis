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

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.users.{UserServiceStub, UserService}
import com.griddynamics.genesis.groups.GroupService
import javax.servlet.http.HttpServletRequest
import GenesisRestController._
import org.springframework.web.bind.annotation._
import com.griddynamics.genesis.api.{ExtendedResult, User}

@Controller
@RequestMapping(Array("/rest/users"))
class UsersController extends RestApiExceptionsHandler {

    @Autowired(required = false) var userServiceBean: UserService = _
    @Autowired(required = false) var groupService: GroupService = _

   private lazy val userService = Option(userServiceBean).getOrElse(UserServiceStub.get)

    @RequestMapping(method = Array(RequestMethod.GET), params = Array("available"))
    @ResponseBody
    def available() = !userService.isReadOnly

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def list() = userService.list

    @RequestMapping(method = Array(RequestMethod.GET), params = Array("tag"))
    @ResponseBody
    def pick(@RequestParam("tag") search: String) =
      userService.search("*" + search + "*").map(item => Map("key" -> item.username, "value" -> item.username))

    @RequestMapping(value = Array("{username:.+}"), method=Array(RequestMethod.GET))
    @ResponseBody
    def user(@PathVariable(value = "username") username: String) = userService.findByUsername(username) match {
        case Some(u) => u
        case None => throw new ResourceNotFoundException("User[username = " + username + "] was not found")
    }

    @RequestMapping(method = Array(RequestMethod.POST))
    @ResponseBody
    def create(request: HttpServletRequest) = RequestReader.read(request) {
        map => 
        val user = readUser(map)
        userService.create(user, readGroups(map, "groups"))
    }

    @RequestMapping(value = Array("{username:.+}"), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(@PathVariable username: String, request: HttpServletRequest) = {
        val params: Map[String, Any] = extractParamsMap(request)
        val userNew = User(username, extractValue("email", params), extractValue("firstName", params),
            extractValue("lastName", params), extractOption("jobTitle", params), None)
        withUser(username) {
            _ => userService.update(userNew, readGroups(params, "groups"))
        }
    }

    @RequestMapping(value = Array("{username:.+}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable(value="username") username: String) = {
      withUser(username) {
        user => userService.delete(user)
      }
    }

  @RequestMapping(value = Array("{userName}/groups"), method = Array(RequestMethod.GET))
  @ResponseBody
  def userGroups(@PathVariable("userName") userName: String) = groupService.getUsersGroups(userName)

    def withUser(username: String)(block: User => ExtendedResult[User]) = {
      userService.findByUsername(username) match {
        case None => throw new ResourceNotFoundException("User [username = " + username + "] was not found")
        case Some(group) => block(group)
      }
    }

    private def readUser(map: Map[String, Any]) =
        User(extractValue("username", map), extractValue("email", map), extractValue("firstName", map),
             extractValue("lastName", map), extractOption("jobTitle", map), Some(extractValue("password", map)))


    private def readGroups(map: Map[String, Any], paramName: String) : List[String] = {
        map.getOrElse(paramName, List()) match {
            case (x :: xs) => (x :: xs).map(_.toString)
            case _ => List()
        }
    }
}
