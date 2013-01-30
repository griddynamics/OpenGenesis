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

import annotations.{LinkTarget, AddSelfLinks}
import links.CollectionWrapper._
import links.HrefBuilder._
import links.{WebPath, LinkBuilder, ItemWrapper, CollectionWrapper}
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import com.griddynamics.genesis.service.DataBagService
import javax.validation.Valid
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.api.DataBag
import scala.Some

@Controller
@RequestMapping(value = Array("/rest/projects/{projectId}/databags"))
class ProjectDatabagController extends RestApiExceptionsHandler {
  @Autowired
  var databagService: DataBagService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, POST), modelClass = classOf[DataBag])
  def listForProject(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) : CollectionWrapper[ItemWrapper[DataBag]] = {
    def wrapDatabag(databag: DataBag) : ItemWrapper[DataBag] = {
      val top = WebPath(request)
      val wrappedItem: ItemWrapper[DataBag] = databag
      wrappedItem.withLinks(LinkBuilder(top / databag.id.get.toString, LinkTarget.SELF, classOf[DataBag], GET, PUT, DELETE)).filtered()
    }
    databagService.listForProject(projectId).map(wrapDatabag(_))
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateForProject(@PathVariable("projectId") projectId: Int,
                       @PathVariable("databagId") databagId: Int,
                       @RequestBody @Valid databag: DataBag) = {
    databagService.update(databag.copy(id = Some(databagId), projectId = Some(projectId)))
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET, PUT, DELETE), modelClass = classOf[DataBag])
  def find(@PathVariable("projectId") projectId: Int, @PathVariable("databagId") databagId: Int, request: HttpServletRequest) : ItemWrapper[DataBag] = {
    get(databagId, projectId)
  }


  def get(databagId: Int, projectId: Int): DataBag = {
    val bag = for {
      databag <- databagService.get(databagId)
      project <- databag.projectId
      if project == projectId
    } yield databag

    bag.getOrElse(throw new ResourceNotFoundException("Couldn't find databag"))
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, @Valid @RequestBody databag: DataBag) = {
    databagService.create(databag.copy(projectId = Some(projectId)))
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable("projectId") projectId: Int, @PathVariable("databagId") databagId: Int) = {
    val bag = get(databagId, projectId)
    databagService.delete(bag)
  }
}
