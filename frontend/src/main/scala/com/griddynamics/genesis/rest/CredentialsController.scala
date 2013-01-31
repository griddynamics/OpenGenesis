/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.rest


import annotations.{AddSelfLinks, LinkTarget}
import links.{WebPath, LinkBuilder, ItemWrapper, CollectionWrapper}
import links.CollectionWrapper._
import links.HrefBuilder._
import scala.Array
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api
import api.Credentials
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.beans.factory.annotation.Autowired
import javax.validation.Valid
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/credentials"))
class CredentialsController extends RestApiExceptionsHandler {

  @Autowired var service: CredentialsStoreService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, @Valid @RequestBody creds: api.Credentials) = {
      service.create(creds).map(_.copy(credential = None))
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, POST), modelClass = classOf[Credentials])
  def listCredentials(@PathVariable("projectId") projectId: Int,
                      @RequestParam(value = "type", required = false) credentialsType: String,
                       request: HttpServletRequest): CollectionWrapper[ItemWrapper[Credentials]] = {
    val creds = if(credentialsType == null) {
      service.list(projectId)
    } else {
      service.findCredentials(projectId, credentialsType)
    }
    def wrapCredential(cred: Credentials) : ItemWrapper[Credentials] = {
      val wrapper: ItemWrapper[Credentials] = cred
      wrapper.withLinks(LinkBuilder(WebPath(request) / cred.id.get.toString, LinkTarget.SELF,
        classOf[Credentials], GET, DELETE)).filtered()
    }
    val cleared: CollectionWrapper[ItemWrapper[Credentials]] = creds.map(_.copy(credential = None)).map(wrapCredential(_))
    cleared
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int) = service.delete(projectId, id)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT), modelClass = classOf[Credentials])
  def getCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") credId: Int, request: HttpServletRequest): ItemWrapper[Credentials] = {
    val cred = service.get(projectId, credId).getOrElse(
      throw new ResourceNotFoundException("Credential [id = %d] was not found in Project [id = %d]".format(credId, projectId))
    )
    cred.copy(credential = None)
  }

}