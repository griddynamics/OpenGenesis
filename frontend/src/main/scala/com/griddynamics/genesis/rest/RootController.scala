package com.griddynamics.genesis.rest

import annotations.LinkTarget
import links.{LinkBuilder, HrefBuilder, ControllerClassAggregator}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.{SystemSettings, Link, Project}
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.{ServletContext, ServletConfig}
import com.griddynamics.genesis.users.GenesisRole
import com.griddynamics.genesis.service.{GenesisSystemProperties, EnvironmentAccessService}
import ControllerClassAggregator.{apply => collectLinks}

@Controller
@RequestMapping(value = Array("/rest"))
class RootController {
  @Autowired implicit var config: ServletConfig = _

  @Autowired var servletContext: ServletContext = _

  @Autowired var envSecurityService: EnvironmentAccessService = _


  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def root(request: HttpServletRequest) = {
    implicit val req = request

    val links: Seq[Link] =
      collectLinks(classOf[ProjectsController], classOf[Project], LinkTarget.COLLECTION) ++
      (if (isAdminOrReadOnly) collectLinks(classOf[SettingsController], classOf[SystemSettings], LinkTarget.COLLECTION) else Seq()) ++
      (if (!isLogoutDisabled) Seq(LinkBuilder(HrefBuilder.absolutePath("logout"), LinkTarget.LOGOUT, RequestMethod.GET)) else Seq())

    Map(
      "user" -> GenesisRestController.getCurrentUser,
      "administrator" -> request.isUserInRole(GenesisRole.SystemAdmin.toString),
      "configuration" -> Map(
        "locale" -> (request.getLocale.getLanguage + "-" + request.getLocale.getCountry),
        "environment_security_enabled" -> envSecurityService.restrictionsEnabled
      ),
      "links" -> links
    )
  }

  private def isLogoutDisabled = {
    "false".equalsIgnoreCase(servletContext.getInitParameter(GenesisSystemProperties.LOGOUT_ENABLED))
  }

  private def isAdminOrReadOnly(implicit request: HttpServletRequest) = {
    request.isUserInRole(GenesisRole.SystemAdmin.toString) || request.isUserInRole(GenesisRole.ReadonlySystemAdmin.toString)
  }
}
