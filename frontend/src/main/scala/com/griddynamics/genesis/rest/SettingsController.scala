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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */

package com.griddynamics.genesis.rest

import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.ConfigService
import com.griddynamics.genesis.rest.GenesisRestController.{extractParamsMap, paramToOption}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.{Failure, Success}

@Controller
@RequestMapping(value = Array("/rest/settings"))
class SettingsController(configService: ConfigService) extends RestApiExceptionsHandler {

    @RequestMapping(method = Array(RequestMethod.GET))
    @ResponseBody
    def listSettings(@RequestParam(value = "prefix", required = false) prefix: String) = configService.listSettings(paramToOption(prefix))

    @RequestMapping(value = Array("{key:.+}"), method = Array(RequestMethod.PUT))
    @ResponseBody
    def update(@PathVariable("key") key: String, request: HttpServletRequest) = using { _ =>
        configService.update(key, extractParamsMap(request)("value"))
    }

    @RequestMapping(value = Array("{key:.+}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def delete(@PathVariable("key") key: String) = using ( _ => configService.delete(key) )

    @RequestMapping(method = Array(RequestMethod.DELETE))
    @ResponseBody
    def clear(@RequestParam(value = "prefix", required = false) prefix: String) = using{ _ => configService.clear(Option(prefix))}

    private def using (block : Any => Any) = {
        try {
            block()
            Success(None)
        } catch {
            case e => Failure(compoundServiceErrors = Seq(e.getMessage))
        }
    }
}
