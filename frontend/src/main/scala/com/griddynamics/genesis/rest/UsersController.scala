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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */

package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.api.User
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.{ResponseBody, PathVariable, RequestMethod, RequestMapping}
import GenesisRestController._

@Controller
@RequestMapping(Array("/rest/users"))
class UsersController extends RestApiExceptionsHandler {

    @Autowired(required = false) var userService: UserService = _

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def list() = userService.list

    @RequestMapping(value = Array("{username}"), method=Array(RequestMethod.GET))
    @ResponseBody
    def user(@PathVariable(value = "username") username: String) = userService.findByUsername(username) match {
        case Some(u) => u
        case None => throw new ResourceNotFoundException
    }

    @RequestMapping(method = Array(RequestMethod.POST))
    @ResponseBody
    def create(request: HttpServletRequest) = {
        val params: Map[String, Any] = extractParamsMap(request)
        val user: User = User(extractValue("username", params), extractValue("email", params), extractValue("lastName", params),
            extractValue("firstName", params), extractOption("jobTitle", params), Some(extractValue("password", params)))
        userService.create(user)
    }

    @RequestMapping(value = Array("{username}"), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(@PathVariable username: String, request: HttpServletRequest) = {
        val params: Map[String, Any] = extractParamsMap(request)
        val user: User = User(username, extractValue("email", params), extractValue("lastName", params),
            extractValue("firstName", params), extractOption("jobTitle", params), None)
        userService.update(user)
    }
}
