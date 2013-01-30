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
import links._
import HrefBuilder._
import CollectionWrapper._
import links.WebPath
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.service.DataBagService
import scala.Array
import org.springframework.beans.factory.annotation.Autowired
import javax.validation.Valid
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.api.DataBag
import scala.Some

@Controller
@RequestMapping(value = Array("/rest/databags"))
class DatabagController extends RestApiExceptionsHandler {

  @Autowired var service: DataBagService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def createDataBag(@Valid @RequestBody databag: DataBag, request: HttpServletRequest) = service.create(databag)

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateDataBag(@Valid @RequestBody databag: DataBag, @PathVariable("databagId") id: Int) = service.update(databag.copy(id = Some(id)))

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteDataBag(@PathVariable("databagId") id: Int) = {
    val bag = findDataBag(id)
    service.delete(bag)
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getDataBag(@PathVariable("databagId") id: Int, request: HttpServletRequest) = {
    implicit val req: HttpServletRequest = request
    wrap(findDataBag(id)).withLinksToSelf(GET, PUT, DELETE).filtered()
  }

  def findDataBag(id: Int): DataBag = {
    service.get(id).getOrElse(throw new ResourceNotFoundException("Couldn't find databag"))
  }

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listDataBags(request: HttpServletRequest) = {
    implicit val req: HttpServletRequest = request
    val top = WebPath(req)
    wrapCollection(
      service.list.map(databag => {
        val wrappedItem: ItemWrapper[DataBag] = wrap(databag)
        wrappedItem.withLinks(LinkBuilder(top / databag.id.get.toString, LinkTarget.SELF, classOf[DataBag], GET, PUT, DELETE)).filtered()
      })
    ).withLinksToSelf(classOf[DataBag], GET, POST).filtered()
  }
}
