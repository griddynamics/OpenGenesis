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

import annotations.{AddSelfLinks, LinkTarget}
import links.CollectionWrapper._
import links.{CollectionWrapper, ItemWrapper, WebPath, LinkBuilder}
import links.HrefBuilder._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.service.{CredentialsStoreService, ServersLoanService, ServersService}
import com.griddynamics.genesis.rest.GenesisRestController._
import com.griddynamics.genesis.api
import api.{ServerArray, ServerDescription, ExtendedResult}
import org.springframework.beans.factory.annotation.Autowired
import javax.validation.Valid
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/server-arrays"))
class ServersController extends RestApiExceptionsHandler {

  @Autowired var service: ServersService = _
  @Autowired var loanService: ServersLoanService = _
  @Autowired var credService: CredentialsStoreService =_
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, @Valid @RequestBody array: ServerArray) = {
    service.create(array.copy(projectId = projectId))
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, POST), modelClass = classOf[ServerArray])
  def list(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) : CollectionWrapper[ItemWrapper[ServerArray]] = {
    implicit val req = request
    def wrapServer(server: ServerArray) = {
      val top = WebPath(request)
      wrap(server).withLinks(LinkBuilder(top / server.id.get.toString, LinkTarget.SELF, server.getClass, PUT, DELETE, GET)).filtered()
    }
    wrapCollection(service.list(projectId).map(wrapServer(_)))
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT, DELETE), modelClass = classOf[ServerArray])
  def get(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, request: HttpServletRequest) : ItemWrapper[ServerArray] = {
    find(projectId, id)
  }

  private def find(projectId: Int, id: Int) = service.get(projectId, id).getOrElse(throw new ResourceNotFoundException("Server array wasn't found in project"))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, request: HttpServletRequest) = {
    service.delete(find(projectId, id))
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateServerArray(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int, @Valid @RequestBody request: ServerArray) = {
    val array = find(projectId, id).copy(
      name = request.name,
      description = request.description
    )
    service.update(array)
  }

  @RequestMapping(value = Array("{arrayId}/servers"), method = Array(RequestMethod.POST))
  @ResponseBody
  def addServer(@PathVariable("projectId") projectId: Int, @PathVariable("arrayId") arrayId: Int, request: HttpServletRequest) = {
    assertArrayBelongsToProject(projectId, arrayId)
    val server = extractServer(request, projectId, arrayId, None)

    server.credentialsId.foreach { assertCredentialsExistInProject(projectId, _) }
    service.create(server)
  }

  @RequestMapping(value = Array("{arrayId}/servers"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getServersInArray(@PathVariable("projectId") projectId: Int, @PathVariable("arrayId") arrayId: Int): Seq[api.Server] = {
    assertArrayBelongsToProject(projectId, arrayId)

    service.getServers(arrayId)
  }

  @RequestMapping(value = Array("{arrayId}/servers/{serverId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteServer(@PathVariable("projectId") projectId: Int,
                   @PathVariable("arrayId") arrayId: Int,
                   @PathVariable("serverId") serverId: Int): ExtendedResult[_] = {
    assertArrayBelongsToProject(projectId, arrayId)
    service.deleteServer(arrayId, serverId)
  }

  @RequestMapping(value = Array("{arrayId}/servers/{serverId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getServerDescription(@PathVariable("projectId") projectId: Int,
                   @PathVariable("arrayId") arrayId: Int,
                   @PathVariable("serverId") serverId: Int,
                   request: HttpServletRequest): ServerDescription = {
    assertArrayBelongsToProject(projectId, arrayId)
    val server = service.getServer(arrayId, serverId).getOrElse(throw new ResourceNotFoundException("Couldn't find server in the array"))
    val envs = loanService.debtorEnvironments(server)
    ServerDescription(server.id, server.arrayId, server.instanceId, server.address, envs)
  }

  private[this] def assertCredentialsExistInProject(projectId: Int, credentialsId: Int) {
    credService.get(projectId, credentialsId).getOrElse(throw new ResourceNotFoundException("Failed to find credentials record in project"))
  }

  private[this] def assertArrayBelongsToProject(projectId: Int, arrayId: Int) {
    service.get(projectId, arrayId).getOrElse(throw new ResourceNotFoundException("Server array wasn't found in project"))
  }

  private[this] def extractServer(request: HttpServletRequest, projectId: Int, arrayId: Int, id: Option[Int]) = {
    val params = extractParamsMap(request)
    val address = extractValue("address", params)
    val instanceId = extractOption("instanceId", params)
    val credentialsId = extractOption("credentialsId", params).flatMap(id => if (id.isEmpty) None else Some(id.toInt))
    if (instanceId.isDefined && !instanceId.get.isEmpty) {
      new api.Server(id, arrayId, instanceId.get, address, credentialsId)
    } else {
      new api.Server(id, arrayId, address, credentialsId)
    }
  }
}
