package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMethod, ResponseBody, RequestMapping}
import javax.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.frontend.GenesisRestService
import com.griddynamics.genesis.rest.links.{ItemWrapper, CollectionWrapper, LinkBuilder, WebPath}
import com.griddynamics.genesis.rest.links.HrefBuilder._
import com.griddynamics.genesis.rest.links.CollectionWrapper._
import com.griddynamics.genesis.rest.annotations.{AddSelfLinks, LinkTarget}
import org.springframework.web.bind.annotation.RequestMethod._
import com.griddynamics.genesis.api.{WorkflowStats, WorkflowDetails}
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.service.ProjectService

@Controller
@RequestMapping(Array("/rest"))
class WorkflowController extends RestApiExceptionsHandler {
  @Autowired var genesisRestService: GenesisRestService = _
  @Autowired var projectService: ProjectService = _
  @Autowired implicit var linkSecurity: LinkSecurityBean = _

  @ResponseBody
  @RequestMapping(value = Array("/workflow-stats"), method = Array(RequestMethod.GET))
  @AddSelfLinks(methods = Array(GET), modelClass = classOf[WorkflowStats])
  def workflowStats(request: HttpServletRequest) : CollectionWrapper[ItemWrapper[WorkflowStats]] = genesisRestService.workflowStats.map { stat =>
    val top = WebPath(absolutePath(s"/rest/projects/${stat.projectId}")(request))
    stat.withLinks(
      LinkBuilder(top / "workflows/running", LinkTarget.COLLECTION, classOf[WorkflowDetails], GET)
    ).filtered()
  }

  @ResponseBody
  @RequestMapping(value = Array("/projects/{projectId}/workflows/running"))
  def runningWorkflows(@PathVariable(value = "projectId") projectId: Int, request: HttpServletRequest) : List[WorkflowDetails] = {
    val mockProject: Int = projectService.list.headOption.flatMap(p => p.id).getOrElse(1)
    List(WorkflowDetails(mockProject, "mock", "Running", "nodoby", 2, Map("foo" -> "bar"), None, None, Some(0), None))
  }

}
