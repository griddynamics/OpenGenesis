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

import annotations.LinkTarget
import links.CollectionWrapper._
import links.{WebPath, LinkBuilder}
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.RemoteAgentsService
import javax.validation.Valid
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.api.{ConfigPropertyType, RemoteAgent}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import GenesisRestController._

@Controller
@RequestMapping(value = Array("/rest/agents"))
class RemoteAgentController extends RestApiExceptionsHandler {
    @Autowired
    var service: RemoteAgentsService = _
    @Autowired implicit var linkSecurity: LinkSecurityBean = _

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def list(request: HttpServletRequest) = {
      implicit val req = request
      val top = WebPath(request)
      def wrapAgent(agent: RemoteAgent) = {
        wrap(agent).withLinks(LinkBuilder(top / agent.id.get.toString, LinkTarget.SELF, classOf[RemoteAgent], GET, PUT, DELETE)).filtered()
      }
      wrapCollection(service.list.map(wrapAgent(_))).withLinksToSelf(classOf[RemoteAgent], GET, POST).filtered()
    }

    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
    def get(@PathVariable(value = "id") key: Int, request: HttpServletRequest) = {
      implicit val req = request
      wrap(find(key)).withLinksToSelf(GET, PUT, DELETE).filtered()
    }


    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
    def update(@PathVariable(value = "id") key: Int, @Valid @RequestBody agent: RemoteAgent) = {
        val existing: RemoteAgent = find(key)
        service.update(agent)
    }

    @ResponseBody @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
    def delete(@PathVariable(value = "id") key: Int) = {
        val agent: RemoteAgent = find(key)
        service.delete(agent)
    }

    @ResponseBody @RequestMapping(method = Array(RequestMethod.POST))
    def create(@Valid @RequestBody agent: RemoteAgent) = {
        service.create(agent)
    }

    @ResponseBody @RequestMapping(value = Array("{id}/settings"), method = Array(RequestMethod.GET))
    def getConfiguration(@PathVariable(value = "id") key: Int) = {
      service.getConfiguration(key)
    }

    @ResponseBody @RequestMapping(value = Array("{id}/settings"), method = Array(RequestMethod.PUT))
    def putConfiguration(@PathVariable(value = "id") key: Int, request: HttpServletRequest) = {
      val map = extractParamsMap(request).map({case (key, value) => (key, value.toString)})
      service.putConfiguration(map, key)
    }

    private def find(key: Int) = service.get(key).getOrElse(throw new ResourceNotFoundException("Couldn't find agent"))
}
