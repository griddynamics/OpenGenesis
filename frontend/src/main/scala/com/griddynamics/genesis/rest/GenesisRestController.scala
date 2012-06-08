/**
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

import org.springframework.stereotype.Controller
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation._
import java.io.InputStreamReader
import javax.servlet.http.HttpServletRequest
import java.security.Principal
import java.util.Properties
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import collection.JavaConversions
import com.griddynamics.genesis.service.TemplateService
import com.griddynamics.genesis.api.GenesisService

@Controller
@RequestMapping(Array("/rest"))
class GenesisRestController(genesisService: GenesisService, templateService: TemplateService) extends RestApiExceptionsHandler {

    @Autowired
    @Qualifier("buildInfo")
    var buildInfoProps: Properties = _

    @RequestMapping(value = Array("build-info"), method = Array(RequestMethod.GET))
    @ResponseBody
    def buildInfo = JavaConversions.propertiesAsScalaMap(buildInfoProps).toMap

    @RequestMapping(value = Array("templates"), method = Array(RequestMethod.GET))
    @ResponseBody
    def listTemplates(@RequestParam(required = false) project: String, @RequestParam(required = false) tag: String) =
      paramToOption(project) match {
        case _ => genesisService.listTemplates
    }

  @RequestMapping(value = Array("templates/{templateName}/v{templateVersion:.+}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def templateContent(@PathVariable templateName: String, @PathVariable templateVersion: String) = {
      val content = templateService.templateRawContent(templateName, templateVersion).getOrElse("")
      Map("content" -> content)
    }

    @RequestMapping(value = Array("templates/{templateName}/v{templateVersion:.+}/{workflow}"), method = Array(RequestMethod.POST))
    @ResponseBody
    def partialApply(@PathVariable templateName: String, @PathVariable templateVersion: String, @PathVariable workflow: String, request: HttpServletRequest) = {
        val paramsMap: Map[String, Any] = GenesisRestController.extractParamsMap(request)
        val variables = GenesisRestController.extractVariables(paramsMap)
        genesisService.queryVariables(templateName, templateVersion, workflow, variables).getOrElse(
          throw new ResourceNotFoundException("No variables were found for [template = %s (v%s), workflow = %s]".format(templateName, templateVersion, workflow))
        )
    }

}

object GenesisRestController {
    import net.liftweb.json._

    def extract[B <: AnyRef : Manifest](request: HttpServletRequest): B = {
      implicit val formats = DefaultFormats
      val json = parse(scala.io.Source.fromInputStream(request.getInputStream).getLines().mkString(" "))
      json.extract[B]
    }

    def extractParamsMap(request: HttpServletRequest): Map[String, Any] = {
        try {
            JsonParser.parse(new InputStreamReader(request.getInputStream), false).values.asInstanceOf[Map[String, Any]]
        } catch {
            case _ => throw new InvalidInputException
        }
    }

    def extractParamsMapList(request: HttpServletRequest): List[Map[String, Any]] = {
        try {
            JsonParser.parse(new InputStreamReader(request.getInputStream), false).values.asInstanceOf[List[Map[String, Any]]]
        } catch {
            case _ => throw new InvalidInputException
        }
    }

    def extractParamsList(request: HttpServletRequest): List[String] = {
        try {
            JsonParser.parse(new InputStreamReader(request.getInputStream), false).values.asInstanceOf[List[String]]
        } catch {
            case _ => throw new InvalidInputException
        }
    }

    def extractVariables(paramsMap: Map[String, Any]): Map[String, String] = {
        if (!paramsMap.contains("variables")) return Map[String, String]()
        paramsMap.apply("variables").asInstanceOf[Map[String, String]]
    }

    def getCurrentUser =  {
      SecurityContextHolder.getContext.getAuthentication.asInstanceOf[Principal].getName
    }

    def extractListValue(valueName: String, values: Map[String, Any]): List[String] = {
      val value = values.getOrElse(valueName, throw new MissingParameterException(valueName))
      value match {
        case list: List[_] => list.map(_.toString)
        case _ => throw new InvalidInputException
      }
    }

    def extractMapValue(valueName: String, values: Map[String, Any]): Map[String, Any] = {
      val value = values.getOrElse(valueName, throw new MissingParameterException(valueName))
      value match {
        case map: Map[String,_] => map
        case _ => throw new InvalidInputException
      }
    }

    def extractValue(valueName : String, values : Map[String, Any]) : String = {
        values.get(valueName) match {
            case Some(s) => String.valueOf(s)
            case None => throw new MissingParameterException(valueName)
        }
    }
  
    def extractNotEmptyValue(valueName: String, values: Map[String,  Any]): String = {
        val value = extractValue(valueName, values)
        if (value.isEmpty) {
          throw new MissingParameterException(valueName)
        }
        value
    }

    def extractOption(valueName: String,  values: Map[String, Any]) : Option[String] = {
       values.get(valueName) match {
         case Some(s) => Some(String.valueOf(s))
         case None => None
       }
    }
  
    def paramToOption(param: String) = {
      if (param == null || param.isEmpty)
        None
      else
        Option(param)
    }

}

