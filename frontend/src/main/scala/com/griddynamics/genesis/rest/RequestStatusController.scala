package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, PathVariable, RequestMapping}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.async.{NotFound, Ready, AskLater, ServiceActorFront}
import org.springframework.http.HttpStatus
import com.griddynamics.genesis.util.Logging


@Controller
@RequestMapping(value = Array("/rest/status"))
class RequestStatusController extends RestApiExceptionsHandler with Logging {
    @Autowired var serviceActor: ServiceActorFront = _

    @RequestMapping(value = Array("{uuid}"))
    @ResponseBody
    def status(request: HttpServletRequest, response: HttpServletResponse, @PathVariable uuid: String) = {
       serviceActor.status(uuid) match {
         case AskLater(r, count) => {
           log.debug(s"There is $count queries for $r")
           response.setStatus(HttpStatus.SEE_OTHER.value())
           response.setHeader("Location", s"/rest/status/$uuid")
         }
         case Ready(result) => {
           result match {
             case t: Throwable => throw t
             case other => other
           }
         }
         case NotFound => {
           log.error(s"Cannot find status for uuid $uuid")
           response.sendError(HttpStatus.GONE.value(), "Resource is outdated. Try to resubmit initial request")
         }
       }
    }

}
