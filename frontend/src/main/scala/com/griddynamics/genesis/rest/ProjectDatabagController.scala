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
import org.springframework.web.bind.annotation.{PathVariable, ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.service.DataBagService
import com.griddynamics.genesis.service.impl.ProjectService

@Controller
@RequestMapping(value = Array("/rest/projects/{projectId}/databags"))
class ProjectDatabagController extends RestApiExceptionsHandler {
  @Autowired
  var databagService: DataBagService = _

  @RequestMapping(method = Array(RequestMethod.GET))
  @ResponseBody
  def listForProject(@PathVariable("projectId") projectId: Int) = {
    databagService.listForProject(projectId)
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def updateForProject(@PathVariable("projectId") projectId: Int, @PathVariable("databagId") databagId: Int, request: HttpServletRequest) = {
    val databag = DatabagController.extractDatabag(request, Some(databagId), Some(projectId))
    databagService.update(databag)
  }

    @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def find(@PathVariable("projectId") projectId: Int, @PathVariable("databagId") databagId: Int, request: HttpServletRequest) = {
        val bag = databagService.get(databagId).getOrElse(throw new ResourceNotFoundException("Couldn't find databag"))
        bag.projectId.map(id => {
            if (id == projectId) {
                bag
            } else {
                throw new ResourceNotFoundException("Couldn't find databag")
            }
        }).getOrElse(throw new ResourceNotFoundException("Couldn't find databag"))
    }

  @RequestMapping(method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = {
    val databag = DatabagController.extractDatabag(request, None, Some(projectId))
    databagService.create(databag)
  }

  @RequestMapping(value = Array("{databagId}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def delete(@PathVariable("projectId") projectId: Int, @PathVariable("databagId") databagId: Int, request: HttpServletRequest) = {
    val bag = databagService.get(databagId).getOrElse(throw new ResourceNotFoundException("Couldn't find databag"))
    bag.projectId.map(id => {
        if (id == projectId)
            databagService.delete(bag)
        else
            throw new InvalidInputException
    }).getOrElse(throw new InvalidInputException)
  }
}
