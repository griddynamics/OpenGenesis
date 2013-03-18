package com.griddynamics.genesis.rest

import annotations.{AddSelfLinks, LinkTarget}
import links.{ItemWrapper, CollectionWrapper, LinkBuilder, WebPath}
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.DatabagTemplateService
import com.griddynamics.genesis.repository.DatabagTemplateRepository
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletRequest
import scala.Array
import org.springframework.web.bind.annotation.RequestMethod._
import com.griddynamics.genesis.api.DatabagTemplate
import com.griddynamics.genesis.api.Link
import com.griddynamics.genesis.spring.security.LinkSecurityBean

@Controller
@RequestMapping(Array("/rest/dbtemplates"))
class DatabagTemplatesController extends RestApiExceptionsHandler {
  import links.CollectionWrapper._
  @Autowired var service: DatabagTemplateService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  val existingScopes =  Set(
    DatabagTemplateRepository.EnvironmentScope,
    DatabagTemplateRepository.ProjectScope,
    DatabagTemplateRepository.SystemScope
  )

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET), modelClass = classOf[DatabagTemplate])
  def scopes(request: HttpServletRequest) : CollectionWrapper[Link] = {
    implicit val req: HttpServletRequest = request
    existingScopes.map(scope => {
      LinkBuilder(WebPath(request) / scope, LinkTarget.COLLECTION, classOf[DatabagTemplate], RequestMethod.GET)
    })
  }

  @RequestMapping(value = Array("/{scope}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET), modelClass = classOf[DatabagTemplate])
  def listTemplateForScope(@PathVariable(value = "scope") scope: String, request: HttpServletRequest): CollectionWrapper[ItemWrapper[DatabagTemplate]] = {
    def wrapItem(item: DatabagTemplate) = {
      val wrapped: ItemWrapper[DatabagTemplate] = item
      wrapped.withLinks(LinkBuilder(WebPath(request) / item.id, LinkTarget.SELF, RequestMethod.GET)).filtered()
      wrapped
    }
    if (existingScopes.contains(scope))
      service.list(scope).map(wrapItem(_))
    else
      throw new ResourceNotFoundException(s"Invalid scope: ${scope}")
  }

  @RequestMapping(value = Array("/{scope}/{id}"), method = Array(RequestMethod.GET))
  @ResponseBody
  @AddSelfLinks(methods = Array(GET), modelClass = classOf[DatabagTemplate])
  def getEnvironmentTemplate(@PathVariable(value = "scope") scope: String,
                             @PathVariable(value = "id") id: String, request: HttpServletRequest) : ItemWrapper[DatabagTemplate] = {
    val template = service.get(id).filter(_.scope == scope).getOrElse(throw new ResourceNotFoundException(s"Databag template [id=${id}] not found for scope [${scope}]"))
    template
  }
}
