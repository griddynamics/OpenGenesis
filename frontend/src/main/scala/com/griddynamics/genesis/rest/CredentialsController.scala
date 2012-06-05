package com.griddynamics.genesis.rest


import scala.Array
import javax.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.web.bind.annotation._

@Controller
@RequestMapping(Array("/rest/projects/{projectId}/credentials"))
class CredentialsController(service: CredentialsStoreService) extends RestApiExceptionsHandler {

  @RequestMapping(value = Array(""), method = Array(RequestMethod.POST))
  @ResponseBody
  def create(@PathVariable("projectId") projectId: Int, request: HttpServletRequest) = {
      val creds = extractCredentials(request, projectId, None)
      service.create(creds)
  }

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def listCredentials(@PathVariable("projectId") projectId: Int) = service.list(projectId).map(_.copy(credential = None))

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.DELETE))
  @ResponseBody
  def deleteCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") id: Int) = service.delete(projectId, id)

  @RequestMapping(value = Array("{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  def getCredentials(@PathVariable("projectId") projectId: Int, @PathVariable("id") credId: Int) = service.get(projectId, credId).map(_.copy(credential = None))


  private def extractCredentials(request: HttpServletRequest, projectId: Int, id: Option[Int]): api.Credentials = {
    val params = extractParamsMap(request)
    val credential = extractOption("credential", params)
    val cloudProvider = extractValue("cloudProvider", params)
    val identity = extractValue("identity", params)
    val pairName = extractValue("pairName", params)

    new api.Credentials(id, projectId, cloudProvider, pairName, identity, credential)
  }
}