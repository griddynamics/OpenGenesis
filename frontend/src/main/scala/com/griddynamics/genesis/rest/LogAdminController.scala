package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestBody, ResponseBody, RequestMethod, RequestMapping}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.configuration.LoggerContext
import scala.Array
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.service.LoggerService

@Controller
@RequestMapping(value = Array("/rest/logpump"))
class LogAdminController {

  @Autowired var loggerContext: LoggerContext = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def pump(request: HttpServletRequest): Map[String, String] = {
    val payload: Map[String,Any] = GenesisRestController.extractParamsMap(request)
    val pair = for (
      logFrom <- payload.get("from");
      logTo <- payload.get("to")
    ) yield (logFrom, logTo)
    pair match {
      case Some((lf: String, lt: String)) => {
         loggerContext.findLoggerService(lf).map {
           sf => loggerContext.findLoggerService(lt).map {
             st => sf.iterate(st)
           }
         }
         Map("ok" -> "true")
      }
      case _ => Map("ok" -> "false")
    }
  }

}
