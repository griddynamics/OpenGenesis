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

import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.ConfigService
import com.griddynamics.genesis.service.GenesisSystemProperties.{PREFIX, PLUGIN_PREFIX}
import com.griddynamics.genesis.rest.GenesisRestController.{extractParamsMap, paramToOption}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.{Failure, Success}
import org.springframework.beans.factory.annotation.Autowired

@Controller
@RequestMapping(value = Array("/rest/settings"))
class SettingsController extends RestApiExceptionsHandler {

    @Autowired var configService: ConfigService = _
    private val VISIBLE_PREFIXES = Seq(PREFIX, PLUGIN_PREFIX)

    private def isVisible(key: String) = VISIBLE_PREFIXES.map(key.startsWith(_)).reduce(_ || _)

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def listSettings(@RequestParam(value = "prefix", required = false) prefix: String) =
        configService.listSettings(paramToOption(prefix)).filter(p => isVisible(p.name))

    @RequestMapping(value = Array("{key:.+}"), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(@PathVariable("key") key: String, request: HttpServletRequest) = using { _ =>
        validKey(key) { k =>
          configService.update(k, extractParamsMap(request)("value"))
        }
    }

    @RequestMapping(value = Array("{key:.+}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable("key") key: String) = using { _ =>
      validKey(key) { k=>
        configService.delete(k)
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
            block()
            Success(None)
        } catch {
            case e: ResourceNotFoundException => Failure(compoundServiceErrors = Seq(e.msg), isNotFound = true)
            case ex => Failure(compoundServiceErrors = Seq(ex.getMessage))
        }
    }

    private def validKey(key: String)(block: String => Any) {
       if (!isVisible(key)) throw new ResourceNotFoundException("Key %s is not found".format(key))
       configService.get(key) match {
         case Some(v) => block(key)
         case None => throw new ResourceNotFoundException("Key %s is not found".format(key))
       }
    }
}
