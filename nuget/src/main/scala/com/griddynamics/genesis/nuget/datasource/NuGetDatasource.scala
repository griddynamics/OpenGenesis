package com.griddynamics.genesis.nuget.datasource

import com.griddynamics.genesis.template.{DataSourceFactory, VarDataSource}
import com.griddynamics.genesis.util.{Closeables, Logging}
import java.net.{URI, URL}
import scala.collection.JavaConversions._
import javax.ws.rs.core.UriBuilder
import org.apache.abdera.Abdera
import java.io.{InputStream, StringReader}
import org.apache.abdera.model.{Entry, Document, Feed}
import javax.xml.namespace.QName
import org.apache.axiom.om.OMContainer
import scala.annotation.tailrec
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api.Credentials
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.HttpHost
import org.apache.http.auth.params.AuthPNames
import org.apache.http.client.params.AuthPolicy
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import scala.io.Source

class NuGetDataSource(credStore: CredentialsStoreService) extends VarDataSource with Logging {
  import NuGetDataSource._
  import FeedConstants._
  import Authentication._
  private var url: URL = _
  private var `type`: String = _
  private var id: String = _
  private var max: Int = _
  private val abdera = new Abdera()
  private var auth: Option[Authentication] = None
  private var credOpt: Option[Credentials] = None

  def getData: Map[String, String] = filter(id)

  def getResults(uri: URI): Seq[(String, String)] = {
    @tailrec
    def getResults(uri: URI, acc: Seq[(String, String)]): Seq[(String, String)] = {

      def get(uri: URI): String = {
        val httpClient = client
        val get = new HttpGet(uri)
        val response = httpClient.execute(get)
        try {
          val code: Int = response.getStatusLine.getStatusCode
          if (code != 200) {
            throw new IllegalStateException(s"Nuget DS: got a status code ${code}, but supposed to get 200")
          }
          val content: InputStream = response.getEntity.getContent
          Closeables.using(content) {c =>
            Source.fromInputStream(c).getLines().mkString("\n")
          }
        } finally {
          EntityUtils.consume(response.getEntity)
        }
      }

      def entry(e: Entry) = {
        val pkg: Option[OMContainer] = Option(e.getExtension(Metadata))
        pkg.map(p => {
          val version: String = p.getFirstChildWithName(VersionQname).getText
          //nuget api v1 has Id attr, v2 delegates it to title
          val entryId: String = p.getFirstChildWithName(IdQname) match {
            case null => e.getTitle
            case value => value.getText
          }
          ( e.getContentElement.getSrc.toString, entryId + " " + version)
        })
      }
      val feedAsString: String = get(uri)
      log.trace(s"Request result: $feedAsString")
      val doc: Document[Feed] = abdera.getParser.parse(new StringReader(feedAsString))
      val feed = doc.getRoot
      val result = acc ++ (for (e <- feed.getEntries.reverse) yield entry(e)).toList.flatten
      //pagination may be possible
      Option(feed.getLink("next")) match {
        case None => result
        case Some(link) => getResults(link.getResolvedHref.toURI, result)
      }
    }
    getResults(uri, Seq())
  }

  private def client = {
    import scala.collection.JavaConversions.seqAsJavaList
    val httpClient = new DefaultHttpClient()
    val host = new HttpHost(url.getHost, url.getPort, url.toURI.getScheme)
    auth match {
      case Some(_) => {
        credOpt.map(credential =>  {
          val decrypted = credStore.decrypt(credential)
          httpClient.getCredentialsProvider.setCredentials(new AuthScope(host.getHostName, host.getPort),
            new UsernamePasswordCredentials(credential.identity, decrypted.credential.getOrElse("")))
        }).orElse(throw new IllegalArgumentException("Datasource is set use authorization, but no credentials found."))
        httpClient.getParams.setParameter(AuthPNames.TARGET_AUTH_PREF, seqAsJavaList(List(AuthPolicy.DIGEST, AuthPolicy.BASIC)))
      }
      case _ =>
    }
    httpClient
  }

  def filter(id: String): Map[String, String] = {
    val builder = UriBuilder.fromUri(url.toURI)
    val initialUri =
      builder.path(`type`).queryParam("$filter", s"Id eq '$id'").queryParam("$top", max.toString).queryParam("$orderby", "Version").build()
    getResults(initialUri).toMap
  }

  def config(cfg: Map[String, Any]) {
    url = new URL(cfg(Url).toString)
    `type` = cfg.get(EntityType).map(_.toString).getOrElse(DefaultType)
    id = cfg(ArtifactId).toString
    max = cfg.get(MaxResults).map(_.asInstanceOf[Int]).getOrElse(DefaultMax)
    auth = cfg.get(AuthMode).map(v => Authentication.withName(v.toString))
    val projId = cfg.get(ProjectId).map(_.asInstanceOf[Int])
    val pairName: String = cfg.get(Credential).getOrElse("").toString
    credOpt = projId.flatMap(credStore.find(_, Mode, pairName))
  }
}

object Authentication extends Enumeration {
  type Authentication = Value
  val none, basic, digest = Value
}

object FeedConstants {
  val MetadataNs: String = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata"
  val Metadata = new QName(MetadataNs, "properties")
  val PropertiesNs = "http://schemas.microsoft.com/ado/2007/08/dataservices"
  val VersionQname = new QName(PropertiesNs, "Version", "")
  val IdQname = new QName(PropertiesNs, "Id", "")
}
object NuGetDataSource {
  val Url = "url"
  val EntityType = "type"
  val ArtifactId = "artifactId"
  val MaxResults = "maxResults"
  val DefaultType = "Packages"
  val DefaultMax = 100
  val DefaultDebug = false
  val AuthMode = "auth"
  val Mode = "nuget"
  val Credential = "credential"
  val ProjectId = "projectId"
}

class NuGetDataSourceFactory(credStore: CredentialsStoreService) extends DataSourceFactory {
  val mode = NuGetDataSource.Mode
  def newDataSource: VarDataSource = new NuGetDataSource(credStore)
}
