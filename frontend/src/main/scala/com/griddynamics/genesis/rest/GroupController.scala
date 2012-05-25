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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */

package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.groups.GroupService
import javax.servlet.http.HttpServletRequest
import GenesisRestController._
import org.springframework.web.bind.annotation._
import com.griddynamics.genesis.api.{ExtendedResult, UserGroup}

@Controller
@RequestMapping(Array("/rest/groups"))
class GroupController extends RestApiExceptionsHandler {
    @Autowired(required = false) var groupService: GroupService = _

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def list() = groupService.list

    @RequestMapping(value=Array("{id}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def get(@PathVariable(value = "id") id: Int) = groupService.get(id)

    @RequestMapping(method = Array(RequestMethod.GET), params = Array("tag"))
    @ResponseBody
    def pick(@RequestParam("tag") search: String) = groupService.search("*" + search + "*").map(item => Map("key" -> item.name, "value" -> item.name))

    @RequestMapping(method = Array(RequestMethod.POST))
    @ResponseBody
    def create(request: HttpServletRequest) = {
        RequestReader.read(request) {
            map => {
                val group: UserGroup = readGroup(map)
                val create = groupService.create(group, readUsers(map, "users"))
                create
            }
        }
    }

    @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(@PathVariable(value="id") id: Int, request: HttpServletRequest)  = {
        RequestReader.read(request) {
            map => {
                withGroup(id) {
                    group => groupService.update(readGroup(map), readUsers(map, "users"))
                }
            }
        }
    }

    @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable(value="id") id: Int)  = {
        withGroup(id) {
            group => groupService.delete(group)
        }
    }

    def withGroup(id: Int)(block: UserGroup => ExtendedResult[_]) = {
        groupService.get(id) match {
            case None => throw new ResourceNotFoundException
            case Some(group) => block(group)
        }
    }

    @RequestMapping(value=Array("{id}/users"), method=Array(RequestMethod.GET))
    @ResponseBody
    def listUsers(@PathVariable(value="id") id: Int) = groupService.users(id)

    @RequestMapping(value=Array("{id}/users"), method=Array(RequestMethod.POST))
    @ResponseBody
    def addUser(@PathVariable(value="id") id: Int, request: HttpServletRequest) = {
        RequestReader.read(request) {
            map => {
                val username = extractValue("username", map)
                withGroup(id) {
                    _ => {
                        groupService.users(id).filter(_.username == username).headOption match {
                            case Some(s) => throw new ResourceConflictException
                            case None => groupService.addUserToGroup(id, username)
                        }
                    }
                }
            }
        }
    }

    @RequestMapping(value=Array("{id}/users"), method=Array(RequestMethod.DELETE))
    @ResponseBody
    def removeUser(@PathVariable(value="id") id: Int, request: HttpServletRequest) = {
        RequestReader.read(request) {
            map => {
                withGroup(id) { _ => groupService.removeUserFromGroup(id, extractValue("username", map)) }
            }
        }
    }

    private def readGroup(map: Map[String, Any]): UserGroup = {
        UserGroup(extractValue("name", map),
            extractValue("description", map),
            extractOption("mailingList", map),
            extractOption("id", map).map(_.toInt)
        )
    }

    private def readUsers(map: Map[String, Any], paramName: String) : List[String] = {
        map.getOrElse(paramName, List()) match {
            case (x :: xs) => (x :: xs).map(_.toString)
            case _ => List()
        }
    }

}
