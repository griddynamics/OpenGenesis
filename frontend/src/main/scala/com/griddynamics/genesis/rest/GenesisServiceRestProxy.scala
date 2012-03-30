package com.griddynamics.genesis.rest

import com.sun.jersey.api.client.{GenericType, Client}
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider
import java.lang.Class
import java.lang.annotation.Annotation
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import javax.ws.rs.{Consumes, Produces}
import javax.ws.rs.ext.Provider
import java.io.{InputStreamReader, Reader, OutputStream, InputStream}
import java.lang.reflect.{ParameterizedType, Type}
import net.liftweb.json.{Extraction, JsonParser}
import net.liftweb.json.JsonDSL._
import net.liftweb.json.{render, compact}
import com.griddynamics.genesis.api._

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

@Provider
@Produces (Array ("*/*") )
@Consumes(Array("*/*"))
class LiftJsonClientProvider extends AbstractMessageReaderWriterProvider[Object]{
  val untouchables = List(classOf[String], classOf[InputStream], classOf[Reader])
  val classMap = Map[AnyRef, AnyRef](classOf[collection.immutable.Seq[_]] -> classOf[List[_]])
  implicit val formats = net.liftweb.json.DefaultFormats

  def isWriteable(p1: Class[_], p2: Type, p3: Array[Annotation], p4: MediaType) = false

  def writeTo(p1: Object, p2: Class[_], p3: Type, p4: Array[Annotation], p5: MediaType, p6: MultivaluedMap[String, AnyRef], p7: OutputStream) {}

  def isReadable(klass: Class[_], genericType: Type, annotations: Array[Annotation], mediaType: MediaType) = {
     untouchables.find(k => k.isAssignableFrom(klass)).size == 0
  }

  def readFrom(klass: Class[Object], genericType: Type, annotations: Array[Annotation], mediaType: MediaType, headers: MultivaluedMap[String, String],
               entityStream: InputStream) : Object = {
    import LiftJsonClientProvider._
    Extraction.extract(JsonParser.parse(new InputStreamReader(entityStream), true))(formats, manifest(genericType))
  }

  
}

object LiftJsonClientProvider {
  val classMap = Map[AnyRef, AnyRef](classOf[collection.immutable.Seq[_]] -> classOf[List[_]])
  def manifest(genericType: Type): Manifest[AnyRef] = {
    genericType match {
      case pt: ParameterizedType => {
        val headArg = manifest(pt.getActualTypeArguments.head)
        val tailArgs = pt.getActualTypeArguments.tail.map(manifest(_))
        val clazz = classMap.getOrElse(pt.getRawType, pt.getRawType).asInstanceOf[Class[AnyRef]]
        Manifest.classType(clazz, headArg, tailArgs: _ *)
      }
      case klass: Class[_] => Manifest.classType(klass)
      case other => throw new IllegalArgumentException("Unexpected type %s".format(other))
    }
  }
}
