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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletRequest
import java.security.Principal
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{ConversionException, TemplateService}
import com.griddynamics.genesis.api.{ExtendedResult, Workflow, Configuration, Failure, GenesisService}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.service

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/templates"))
class GenesisRestController extends RestApiExceptionsHandler with Logging {

    import com.griddynamics.genesis.rest.GenesisRestController._

    @Autowired var genesisService: GenesisService = _
    @Autowired var templateService: TemplateService = _
    @Autowired var configurationService: service.EnvironmentService = _

    @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
    @ResponseBody
    def listTemplates(@PathVariable("projectId")  projectId: Int) = genesisService.listTemplates(projectId)

    @RequestMapping(value = Array("{templateName}/v{templateVersion:.+}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def getTemplate(@PathVariable("projectId") projectId: Int,
                    @PathVariable("templateName") templateName: String,
                    @PathVariable("templateVersion") templateVersion: String,
                    @RequestParam(defaultValue = "desc") format: String
                     ) = {
      val result = try {
          format match {
              case "src" => val contentOpt = templateService.templateRawContent(projectId, templateName, templateVersion)
                  contentOpt.map { src => Map("name" -> templateName, "version" -> templateVersion, "content" -> src)}
              case "desc" => genesisService.getTemplate(projectId, templateName, templateVersion)
          }
      } catch {
          case e: Exception =>
              log.error(e, "Failed to get template %s version %s", templateName, templateVersion)
              Option(Failure(compoundServiceErrors = List(e.getMessage), stackTrace = Option(e.getStackTraceString)))
      }
      result.getOrElse(throw new ResourceNotFoundException("Template not found"))
    }

    @RequestMapping(value = Array("{templateName}/v{templateVersion:.+}/{name}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def getWorkflow(@PathVariable("projectId") projectId: Int,
                    @PathVariable("templateName") templateName: String,
                    @PathVariable("templateVersion") templateVersion: String,
                    @PathVariable("name")name: String,
                    @RequestParam(value = "configurationId", required = false) configId: java.lang.Integer): ExtendedResult[Workflow] = {
        val configuration: Int = Option(configId).map(_.toInt).getOrElse(
          getConfigId(projectId, "configurationId")
        )
        try {
            genesisService.getWorkflow(projectId, configuration, templateName, templateVersion, name)
        } catch {
            case e: Exception =>
                log.error(e, "Failed to get template %s version %s", templateName, templateVersion)
                Failure(compoundServiceErrors = List(e.getMessage), stackTrace = Option(e.getStackTraceString))
        }
    }


    private def getConfigId(projectId: Int, paramName: String): Int = {
      defaultConfig(projectId) match {
        case Left(x) => x.id.get
        case Right(message) => throw new MissingParameterException("configurationId")
      }
    }

    @RequestMapping(value = Array("{templateName}/v{templateVersion:.+}/{workflow}"), method = Array(RequestMethod.POST))
    @ResponseBody
    def partialApply(@PathVariable("projectId") projectId: Int,
                     @PathVariable("templateName") templateName: String,
                     @PathVariable("templateVersion") templateVersion: String,
                     @PathVariable("workflow") workflow: String,
                     request: HttpServletRequest) = {
        val paramsMap: Map[String, Any] = GenesisRestController.extractParamsMap(request)
        val variables = GenesisRestController.extractVariables(paramsMap)
        val configId = extractOption("configurationId", paramsMap).map(_.toInt).getOrElse(getConfigId(projectId, "configurationId"))
        try {
            genesisService.queryVariables(projectId, configId, templateName, templateVersion, workflow, variables)
        } catch {
            case e: ConversionException => log.error(e, "Conversion error"); Failure(variablesErrors = Map(e.fieldId -> e.message))
            case x: Exception => log.error(x, "Unknown error"); Failure(compoundServiceErrors = Seq(x.getMessage))
        }
    }

    private def defaultConfig(projectId: Int): Either[Configuration, String] = {
      configurationService.list(projectId) match {
        case head :: Nil => Left(head)
        case Nil => Right("No environment configurations are available to be used as default")
        case _ => Right("More than one environment configuration available. Exact choice should be provided")
      }
    }
}

object GenesisRestController extends Logging {
    import net.liftweb.json._

    val DEFAULT_CHARSET = "UTF-8"

    def extract[B <: AnyRef : Manifest](request: HttpServletRequest): B = {
      implicit val formats = DefaultFormats
      val json = parse(scala.io.Source.fromInputStream(request.getInputStream, DEFAULT_CHARSET).getLines().mkString(" "))
      json.extract[B]
    }

    private[this] def safeInput[T](body: => T): T = try { body } catch {
      case e: Exception => {
        log.warn("Failed to parse input stream", e)
        throw new InvalidInputException
      }
    }

  private def parseJson(request: HttpServletRequest) = {
    request.setCharacterEncoding(DEFAULT_CHARSET)
    JsonParser.parse(s = request.getReader, closeAutomatically = false).values
  }
    def extractParamsMap(request: HttpServletRequest): Map[String, Any] = safeInput {
        parseJson(request).asInstanceOf[Map[String, Any]]
    }

    def extractParamsMapList(request: HttpServletRequest): List[Map[String, Any]] = safeInput {
        parseJson(request).asInstanceOf[List[Map[String, Any]]]
    }

    def extractParamsList(request: HttpServletRequest): List[String] = safeInput {
        parseJson(request).asInstanceOf[List[String]]
    }

    def extractVariables(paramsMap: Map[String, Any]): Map[String, String] = {
        if (!paramsMap.contains("variables")) return Map[String, String]()
        paramsMap.apply("variables").asInstanceOf[Map[String, String]]
    }

    def getCurrentUser =  {
      SecurityContextHolder.getContext.getAuthentication.asInstanceOf[Principal].getName
    }

    def getCurrentUserAuthorities = {
      import scala.collection.JavaConversions._
      val auth = SecurityContextHolder.getContext.getAuthentication
      auth.getAuthorities.map (_.getAuthority)
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
       values.get(valueName).map(String.valueOf(_))
    }
  
    def paramToOption(param: String) = {
      if (param == null || param.isEmpty)
        None
      else
        Option(param)
    }
}

