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

import org.springframework.web.bind.annotation.{RequestMethod, ResponseBody, RequestMapping}
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.SystemService
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import com.griddynamics.genesis.api.{Link, SystemSettings}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.rest.links.{HrefBuilder, LinkBuilder, WebPath}
import HrefBuilder._
import com.griddynamics.genesis.rest.annotations.LinkTarget

@Controller
@RequestMapping(value = Array("/rest/system"))
class SystemController extends RestApiExceptionsHandler {
  @Autowired(required = false)  @Qualifier("override") private var systemService: SystemService = _
  @Autowired private var defSystemService: SystemService = _

  @Autowired private implicit var linkSecurity: LinkSecurityBean = _

  private lazy val service = Option(systemService).getOrElse(defSystemService)


  @RequestMapping(value = Array("root"), method = Array(RequestMethod.GET)) //TODO: mapping will be changed
  @ResponseBody
  def root(request: HttpServletRequest) = new SystemSettings(
    linkSecurity.filter(collectLinks(request)).toArray
  )

  private def collectLinks(request: HttpServletRequest): Array[Link] = {
    implicit val req: HttpServletRequest = request
    val path: WebPath = WebPath(absolutePath("/rest/system"))
    val result = Array(
      LinkBuilder(path / "stop", LinkTarget.SELF, POST)
    )
    if (service.isRestartable) result :+ LinkBuilder(path / "restart", LinkTarget.SELF, POST) else result
  }

  @RequestMapping(value = Array("restart"),method = Array(RequestMethod.POST))
  @ResponseBody
  def restart() = if (service.isRestartable) service.restart else log.error("Restart is not supported!")

  @RequestMapping(value = Array("stop"),method = Array(RequestMethod.POST))
  @ResponseBody
  def stop() = service.stop

}
