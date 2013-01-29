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

import annotations.LinkTarget._
import links.WebPath
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.ConfigService
import com.griddynamics.genesis.service.GenesisSystemProperties.{PREFIX, PLUGIN_PREFIX}
import com.griddynamics.genesis.rest.GenesisRestController.{extractParamsMap, paramToOption}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api._
import org.springframework.beans.factory.annotation.Autowired
import links.{WebPath, HrefBuilder, Link}
import HrefBuilder._
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.api.ConfigProperty
import com.griddynamics.genesis.api.Failure
import scala.Some
import com.griddynamics.genesis.rest.SystemSettings
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.groups.GroupService

case class SystemSettings(links: Array[Link]) //TODO: move to api after link will be moved

@Controller
@RequestMapping(value = Array("/rest/settings"))
class SettingsController extends RestApiExceptionsHandler {

    import ConfigPasswordHelper._

    @Autowired var configService: ConfigService = _
    @Autowired implicit var linkSecurity: LinkSecurityBean = _
    @Autowired var userService: UserService = _
    @Autowired var groupService: GroupService = _

    private val VISIBLE_PREFIXES = Seq(PREFIX, PLUGIN_PREFIX)

    private def isVisible(key: String) = VISIBLE_PREFIXES.map(key.startsWith(_)).reduce(_ || _)

    @RequestMapping(value = Array("root"), method = Array(RequestMethod.GET)) //TODO: mapping will be changed
    @ResponseBody
    def root(request: HttpServletRequest): SystemSettings = {
      new SystemSettings(linkSecurity.filter(collectLinks(request)).toArray)
    }

    def collectLinks(request: HttpServletRequest): Array[Link] = {
       implicit val req: HttpServletRequest = request
       val path: WebPath = WebPath(absolutePath("/rest"))
       var result = List (
          Link(path / "settings", COLLECTION, classOf[ConfigProperty], GET),
          Link(path / "databags", COLLECTION, classOf[DataBag], GET),
          Link(path / "roles", COLLECTION, GET, POST),
          Link(path / "agents", COLLECTION, classOf[RemoteAgent], GET),
          Link(path / "plugins", COLLECTION, classOf[Plugin], GET)
       )
       if (! userService.isReadOnly) {
         result = Link(path / "users", COLLECTION, classOf[User], GET) :: result
       }
       if (! groupService.isReadOnly) {
         result = Link(path / "groups", COLLECTION, classOf[UserGroup], GET) :: result
       }
       result.toArray
    }

    @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
    @ResponseBody
    def listSettings(@RequestParam(value = "prefix", required = false) prefix: String) = {
      val configs = configService.
        listSettings(paramToOption(prefix)).
        filter(p => isVisible(p.name))

      hidePasswords(configs)
    }

    @RequestMapping(value = Array(""), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(request: HttpServletRequest) = using { _ =>
      val map = extractParamsMap(request).
        filter { case (key, value) => validKey(key) { _.propertyType != ConfigPropertyType.PASSWORD || value != blankPassword  }}

      configService.update(map)
    }

    @RequestMapping(value = Array("{key:.+}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable("key") key: String) = using { _ =>
      validKey(key) { k=>
        configService.delete(k.name)
      }
    }

    @RequestMapping(method = Array(RequestMethod.DELETE))
    @ResponseBody
    def clear(@RequestParam(value = "prefix", required = false) prefix: String) = using{ _ =>
        prefix match {
            case p: String if (isVisible(p)) => configService.clear(Option(p))
            case _ => throw new IllegalArgumentException("Only system or plugin properties could be deleted.")
        }
    }

    private def using (block : Any => Any) = {
        try {
          block() match {
            case er: ExtendedResult[_] => er
            case r => Success(r)
          }
        } catch {
            case e: ResourceNotFoundException => Failure(compoundServiceErrors = Seq(e.msg), isNotFound = true)
            case ex: Throwable => Failure(compoundServiceErrors = Seq(ex.getMessage))
        }
    }

    @RequestMapping(value = Array("restart"), method = Array(RequestMethod.GET))
    @ResponseBody
    def restartRequired() = configService.restartRequired()


    private def validKey[T](key: String)(block: ConfigProperty => T): T = {
       if (!isVisible(key)) throw new ResourceNotFoundException("Key %s is not found".format(key))
       configService.getPropertyWithMeta(key) match {
         case Some(v) => block(v)
         case None => throw new ResourceNotFoundException("Key %s is not found".format(key))
       }
    }
}
