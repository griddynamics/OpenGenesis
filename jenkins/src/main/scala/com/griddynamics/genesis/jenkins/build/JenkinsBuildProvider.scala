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
package com.griddynamics.genesis.jenkins.build

import com.griddynamics.genesis.build.{BuildLogEntry, BuildResult, BuildProvider, BuildSpecification}
import java.io.InputStream
import xml.{Elem, XML}
import com.griddynamics.genesis.jenkins.api.{JenkinsRemoteApi, JenkinsConnectSpecification}
import com.griddynamics.genesis.util.Logging

class JenkinsBuildProvider extends BuildProvider with Logging {
  type BuildIdType = Int

  override val mode = "jenkins"

  private def connSpec(values: Map[String, String]) = JenkinsConnectSpecification(values("url"), values.get("username"), values.get("password"))

  private def api(values: Map[String, String]) = new JenkinsRemoteApi(connSpec(values))

  override def build(values: Map[String, String]) = {
    val spec = Specifications(values)
    val connSpc = connSpec(values)
    val a = api(values)
    val (lastBuild, nextBuildNum) = a.get(connSpc.jobXmlApi(spec.projectName))(nextBuildNumber)
    a.postNoResult(connSpc.buildUrl(spec.projectName))
    nextBuildNum
  }

  override def query(id: BuildIdType, values: Map[String, String]): (Option[JenkinsBuildResult], Seq[BuildLogEntry]) = {
    try {
      val result: JenkinsBuildResult = jobStatus(api(values), connSpec(values), Specifications(values), id)
      result match {
        case r@JenkinsBuildResult(_, false, _) => (None, Seq())
        case r@JenkinsBuildResult(_, true, _) => (Some(r), Seq())
      }
    } catch {
      case e: Exception => {
        log.warn(e, "Couldn't query job status")
        (None, Seq())
      }
    }
  }

  def cancel(id: BuildIdType, values: Map[String, String]) {
    api(values).postNoResult(connSpec(values).stopBuildUrl(Specifications(values).projectName, id))
  }

  def jobStatus(api: JenkinsRemoteApi, connSpec: JenkinsConnectSpecification, spec: BuildSpecification, number: Int) = api.get(connSpec.jobUrl(spec.projectName, number))(buildResult)

  def retry[B](retryCount: Int, delay: Int)(fun: => B): B = {
    try {
      fun
    } catch {
      case e if retryCount > 1 => {
        Thread.sleep(delay * 1000)
        retry(retryCount - 1, delay)(fun)
      }
    }
  }

  def buildResult(s: InputStream) = {
    val xml = XML.load(s)
    val number = (xml \\ "number").text.trim().toInt
    val building = (xml \\ "building").text.trim().toBoolean
    val successful = (xml \\ "result").text.trim().toUpperCase.startsWith("SUCCESS")
    JenkinsBuildResult(successful, !building, number)
  }

  def nextBuildNumber(s: InputStream): (Int, Int) = {
    val xml: Elem = XML.load(s)
    val lastNumber = xml \\ "lastBuild" \ "number"
    val nextNumber = xml \\ "nextBuildNumber"
    (lastNumber.text.toInt, nextNumber.text.toInt)
  }
}

case class JenkinsBuildResult(success: Boolean, completed: Boolean, number: Int) extends BuildResult

object Specifications {
  def apply(values: Map[String, String]) =
    new BuildSpecification {
      def projectName: String = values("projectName")
    }
}