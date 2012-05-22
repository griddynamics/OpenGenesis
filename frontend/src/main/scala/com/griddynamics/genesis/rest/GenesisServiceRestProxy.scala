package com.griddynamics.genesis.rest

import com.sun.jersey.api.client.{GenericType, Client}
import com.sun.jersey.api.client.config.DefaultClientConfig
import javax.ws.rs.core.MediaType
import net.liftweb.json.Extraction
import net.liftweb.json.JsonDSL._
import net.liftweb.json.{render, compact}
import com.griddynamics.genesis.api._
import com.griddynamics.genesis.json.utils.LiftJsonClientProvider

class GenesisServiceRestProxy(val backendUrl: String) extends GenesisService {
  val clientConfig = new DefaultClientConfig()
  clientConfig.getClasses.add(classOf[LiftJsonClientProvider])
  val client = Client.create(clientConfig)

  def getLogs(envName: String, stepId: Int) = {
    val resource = client.resource(backendUrl + "/envs/%s/logs/%d".format(envName, stepId))
      .accept(MediaType.TEXT_PLAIN_TYPE)
    Seq(resource.get(classOf[String]))
  }

  def listEnvs(projectId: Int) = {
    val resource = client.resource(backendUrl + "/envs?projectId=" + projectId).accept(MediaType.APPLICATION_JSON_TYPE)
    val list = resource.get(new GenericType[List[Environment]](){})
    list.toSeq
  }

  def listEnvs(projectId: Int, start: Int, limit: Int) = {
    List()
  }

  def countEnvs(projectId: Int) = 0

  def describeEnv(envName: String) = {
    val resource = client.resource(backendUrl + "/envs/%s".format(envName)).accept(MediaType.APPLICATION_JSON_TYPE)
    try {
      Some(resource.get(classOf[EnvironmentDetails]))
    } catch {
      case e => None
    }
  }

  def listTemplates = {
    val resource = client.resource(backendUrl + "/templates").accept(MediaType.APPLICATION_JSON_TYPE)
    resource.get(new GenericType[List[Template]](){})
  }

  def createEnv(projectId: Int, envName: String, creator: String, templateName: String, templateVersion: String, variables: Map[String, String]) = {
    var post = ("projectId" -> projectId) ~ ("envName" -> envName) ~ ("creator" -> creator) ~ ("templateName" -> templateName) ~ ("templateVersion" -> templateVersion) ~ ("variables" -> Extraction.unflatten(variables.map(e => ("."+e._1, e._2)).toMap))
    val resource = client.resource(backendUrl + "/create")
    val request = compact(render(post))
    resource.post(classOf[RequestResult], request)
  }

  def destroyEnv(envName: String, variables: Map[String, String]) = {
    val resource = client.resource(backendUrl + "/envs/%s".format(envName)).accept(MediaType.APPLICATION_JSON_TYPE)
    resource.delete(classOf[RequestResult])
  }

  def requestWorkflow(envName: String, workflowName: String, variables: Map[String, String]) = {
    val params = ("variables" -> Extraction.unflatten(variables.map(e => ("."+e._1, e._2)).toMap))
    val resource = client.resource(backendUrl + "/exec/%s/%s".format(envName, workflowName))
    resource.post(classOf[RequestResult], compact(render(params)))
  }

  def cancelWorkflow(envName: String) {
    val resource = client.resource(backendUrl + "/cancel/%s".format(envName)).accept(MediaType.APPLICATION_JSON_TYPE)
    resource.post()
  }

  def listProjects() = {
    val resource = client.resource(backendUrl + "/projects").accept(MediaType.APPLICATION_JSON_TYPE)
    resource.get(new GenericType[List[Map[String, String]]](){}).map(m => (m("id") -> m("name"))).toMap
  }

  def listTags(projectId: String) = {
    val resource = client.resource(backendUrl + "/projects/%s/tags".format(projectId)).accept(MediaType.APPLICATION_JSON_TYPE)
    resource.get(new GenericType[List[Map[String,String]]](){}).map(m => m("id")).toSeq
  }

  def listTemplates(projectId: String, tagId: String) = {
    val resource = client.resource(backendUrl + "/projects/%s/tags/%s/templates".format(projectId, tagId)).accept(MediaType.APPLICATION_JSON_TYPE)
    resource.get(new GenericType[List[Template]](){})
  }

  def listTemplates(projectId: String) = {
    val resource = client.resource(backendUrl + "/projects/%s/templates".format(projectId)).accept(MediaType.APPLICATION_JSON_TYPE)
    resource.get(new GenericType[List[Template]](){})
  }
}


