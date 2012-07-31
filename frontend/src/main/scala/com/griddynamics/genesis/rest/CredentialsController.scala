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


import scala.Array
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.Autowired

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/credentials"))
class CredentialsController extends RestApiExceptionsHandler {

  @Autowired var service: CredentialsStoreService = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = {
      val creds = extractCredentials(request, projectId, None)
      service.create(creds).map(_.copy(credential = None))
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def listCredentials(@PathVariable("projectId") projectId: Int, @RequestParam(value = "type", required = false) credentialsType: String) = {
    val creds = if(credentialsType == null) {
      service.list(projectId)
    } else {
      service.findCredentials(projectId, credentialsType)
    }
    creds.map(_.copy(credential = None))
  }

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int) = service.delete(projectId, id)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") credId: Int) =
      service.get(projectId, credId).map(_.copy(credential = None)).orElse(
      throw new ResourceNotFoundException("Credential [id = %d] was not found in Project [id = %d]".format(credId, projectId)))


  private def extractCredentials(request: HttpServletRequest, projectId: Int, id: Option[Int]): api.Credentials = {
    val params = extractParamsMap(request)
    val credential = extractOption("credential", params)
    val cloudProvider = extractValue("cloudProvider", params).trim()
    val identity = extractValue("identity", params).trim()
    val pairName = extractValue("pairName", params).trim()

    new api.Credentials(id, projectId, cloudProvider, pairName, identity, credential)
  }
}