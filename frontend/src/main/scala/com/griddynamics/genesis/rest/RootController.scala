package com.griddynamics.genesis.rest

import annotations.LinkTarget
import links.{ControllerClassAggregator, Link}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, RequestMethod, RequestMapping}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.api.Project
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.ServletConfig
import com.griddynamics.genesis.users.GenesisRole

@Controller
@RequestMapping(value = Array("/rest"))
class RootController {
  @Autowired implicit var config: ServletConfig = _

  @RequestMapping(value = Array(""), method = Array(RequestMethod.GET))
  @ResponseBody
  def root(request: HttpServletRequest) = {
    implicit val req = request
    val links = ControllerClassAggregator(classOf[ProjectsController], classOf[Project], LinkTarget.COLLECTION)
    if (request.isUserInRole(GenesisRole.SystemAdmin.toString) || request.isUserInRole(GenesisRole.ReadonlySystemAdmin.toString))
      links ++ ControllerClassAggregator(classOf[SettingsController], classOf[Link], LinkTarget.COLLECTION)
    else
      links
  }

}
