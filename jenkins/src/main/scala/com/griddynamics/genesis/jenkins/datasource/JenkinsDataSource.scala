/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.jenkins.datasource

import com.griddynamics.genesis.template.{DataSourceFactory, VarDataSource}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.cache.Cache
import java.io.InputStream
import io.Source
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json._
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.jenkins.api.{JenkinsRemoteApi, JenkinsConnectSpecification}

class JenkinsDataSource(val cacheManager: CacheManager, val credStore: CredentialsStoreService) extends VarDataSource with Logging with Cache {

  import JenkinsDataSource._

  implicit val formats = DefaultFormats

  private var connectionSpec: JenkinsConnectSpecification = _
  private var artifactFilter: String = _
  private var api: JenkinsRemoteApi = _
  private var jobName: String = _
  private var showBuildNumber: Boolean = _

  override val defaultTtl = 120

  override def config(config: Map[String, Any]) {
    val jenkinsUrl = config(Url).toString
    this.jobName = config(JobName).toString
    this.artifactFilter = config.getOrElse(JenkinsDataSource.ArtifactFilter, ".*").toString

    this.showBuildNumber = config.getOrElse(ShowBuildNumber, false).asInstanceOf[Boolean]

    this.connectionSpec = {
      val credsOption = for {
        projectId <- config.get("projectId").map(_.asInstanceOf[Int])
        credName <- config.get(Credentials).map(_.toString)
        credentialsObj <- credStore.find(projectId, CredProvider, credName).map(credStore.decrypt(_))
      } yield (Some(credentialsObj.identity), credentialsObj.credential)

      val (username, password) = credsOption.getOrElse((None, None))
      new JenkinsConnectSpecification(jenkinsUrl, username, password)
    }

    this.api = new JenkinsRemoteApi(this.connectionSpec)
  }

  override def getData = {
    val arts = fromCache(CacheRegion, CacheKey(connectionSpec, jobName)) {
      loadFromJenkins
    }
    arts.collect {
      case (url, artifact, title) if artifact.matches(artifactFilter) => (title, url)
    }.toMap
  }

  private[this] def loadFromJenkins: List[(String, String, String)] = { // (url, artifact, artifactTitle)
    def artifacts(is: InputStream) = {
      for {
        build <- extractBuilds(is)
        artifact <- build.artifacts
      } yield {
        val artifactName = artifact.fileName
        val downloadUrl = build.url + "/artifact/" + artifact.relativePath

        val title = if (showBuildNumber) {
          "%d : %s %s".format(build.number, artifactName, status(build))
        } else {
          "%s %s".format(artifactName, status(build))
        }

        (downloadUrl, artifactName, title)
      }
    }

    api.get(connectionSpec.buildArtifactsJson(jobName))(artifacts)
  }


  private[this] def status(build: JenkinsBuild): String = {
    try {
      val promotionLevel = build.actions.find(action => !action.obj.isEmpty).map(_.extract[BuildAction].levelValue)
      promotionLevel match {
        case Some(0) => "     (good)"
        case Some(1) => "      (bad)"
        case _ => ""
      }
    } catch {
      case e: Exception => {
        log.warn(e, "Failed to determine build status")
        ""
      }
    }
  }

  private[this] def extractBuilds(is: InputStream) = {
    val str: String = Source.fromInputStream(is).getLines().mkString("")
    val json = parse(str)

    json.extract[Map[String, List[JenkinsBuild]]].apply("builds")
  }

}

object JenkinsDataSource {
  val Url = "url"
  val JobName = "jobName"
  val ArtifactFilter = "filter"
  val Credentials = "credentials"
  val ShowBuildNumber = "showBuildNumber"

  val CredProvider = "jenkins"
  val CacheRegion = "jenkins-data-source"
}

class JenkinsDSFactory(cacheManager: CacheManager, credentialsService: CredentialsStoreService) extends DataSourceFactory {
  val mode = "jenkins"

  def newDataSource = new JenkinsDataSource(cacheManager, credentialsService)
}

private[jenkins] case class JenkinsBuild(actions: List[JObject], artifacts: List[JenkinsArtifact], url: String, number: Int)
private[jenkins] case class JenkinsArtifact(relativePath: String, fileName: String)
private[jenkins] case class BuildAction(levelValue: Int)
private[jenkins] case class CacheKey(spec: JenkinsConnectSpecification, jobName: String)
