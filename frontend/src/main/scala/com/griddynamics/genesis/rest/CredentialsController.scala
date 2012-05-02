package com.griddynamics.genesis.rest


import scala.Array
import com.griddynamics.genesis.api._
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api.RequestResult
import com.griddynamics.genesis.api
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.web.bind.annotation._

@Controller
@RequestMapping(Array("/rest/credentials"))
class CredentialsController(service: CredentialsStoreService) extends RestApiExceptionsHandler {

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.PUT))
  @ResponseBody
  def update(@PathVariable("id") credId: Int, request: HttpServletRequest): RequestResult = {
    val creds = extractCredentials(request, Option(credId))
    service.update(creds)
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def create(request: HttpServletRequest): RequestResult = {
      val creds = extractCredentials(request, None)
      service.create(creds)
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def listCredentials(@RequestParam("projectId") projectId: Int) = service.list(projectId).map(_.copy(credential = None))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteCredentials(@PathVariable("id") id: Int) = service.delete(id)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getCredentials(@PathVariable("id") credId: Int) = service.get(credId).map(_.copy(credential = None))


  private def extractCredentials(request: HttpServletRequest, id: Option[Int]): api.Credentials = {
    val params = extractParamsMap(request)
    val credential = extractOption("credential", params)
    val cloudProvider = extractValue("cloudProvider", params)
    val identity = extractValue("identity", params)
    val projectId = extractValue("projectId", params).toInt
    val pairName = extractValue("pairName", params)

    new api.Credentials(id, projectId, cloudProvider, pairName, identity, credential)
  }
}