package com.griddynamics.genesis.rest


import scala.Array
import com.griddynamics.genesis.api._
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.CredentialsStoreService
import org.springframework.web.bind.annotation._

@Controller
@RequestMapping(Array("/rest/credentials"))
class CredentialsController(service: CredentialsStoreService) extends RestApiExceptionsHandler {

  @RequestMapping(value = Array(""), method = Array(RequestMethod.PUT))
  @ResponseBody
  def createCredentials(request: HttpServletRequest): RequestResult = {
    val creds = GenesisRestController.extract[Credentials](request)
    service.create(creds)
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def updateCredentials(request: HttpServletRequest): RequestResult = {
      val creds = GenesisRestController.extract[Credentials](request)
      service.update(creds)
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def listCredentials(@RequestParam("projectId") projectId: Int) = service.list(projectId)

  @RequestMapping(value = Array(""), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteCredentials(id: Int) = service.delete(id)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getCredentials(@PathVariable("id") credId: Int) = service.get(credId)
}