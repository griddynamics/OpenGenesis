/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.build.jenkins

import com.griddynamics.genesis.build.{BuildResult, BuildSpecification, BuildProvider}
import java.io.InputStream
import xml.{Elem, XML}
import java.lang.IllegalStateException
import java.util.ArrayList
import org.apache.http.auth.params.AuthPNames
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.params.AuthPolicy
import org.apache.http.params.CoreConnectionPNames
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpRequestBase}
import org.apache.http.impl.client.{BasicAuthCache, BasicCredentialsProvider, DefaultHttpClient}
import java.net.URI
import org.apache.http.{HttpHost, HttpEntity, HttpResponse}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.client.protocol.ClientContext
import org.apache.http.protocol.{HttpContext, BasicHttpContext}

class JenkinsBuildProvider(val specification : JenkinsConnectSpecification) extends BuildProvider {

    override val mode = "jenkins"
    var next : Int = _
    var buildSpec : BuildSpecification = _
    override def build(values : Map[String,  String]) { doBuild(Specifications(values)) }
  
    def doBuild(spec : BuildSpecification) {
        buildSpec = spec
        val nb: (Int, Int) = nextBuild(spec)
        next = nb._2
        postNoResult(specification.buildUrl(spec.projectName))
    }
  
    override def query() : Option[JenkinsBuildResult] = {
      try {
        val result : JenkinsBuildResult = jobStatus(buildSpec, next)
        result match {
          case r@JenkinsBuildResult(false, _, _) => None
          case r@JenkinsBuildResult(true, _, _) => Some(r)
        }
      } catch {
        case e => None
      }
    }

    def cancel() {}

    def nextBuild(spec: BuildSpecification) = get(specification.jobXmlApi(spec.projectName))(nextBuildNumber)
    def jobStatus(spec : BuildSpecification, number : Int) = get(specification.jobUrl(spec.projectName, number))(buildResult)
    def retry[B](retryCount : Int, delay : Int)(fun : => B) : B = {
        try {fun} catch {
            case e if retryCount > 1 => {
                Thread.sleep(delay * 1000)
                retry(retryCount - 1, delay)(fun)
            }
        }
    }

    def buildResult(s : InputStream) = {
        val xml = XML.load(s)
        val number = (xml \\ "number").text.trim().toInt
        val building = (xml \\ "building").text.trim().toBoolean
        val successful = (xml \\ "result").text.trim().toUpperCase.startsWith("SUCCESS")
        JenkinsBuildResult(successful, ! building, number)
    }

    def nextBuildNumber(s : InputStream) : (Int, Int) = {
        val xml: Elem = XML.load(s)
        val lastNumber = xml \\ "lastBuild" \ "number"
        val nextNumber = xml \\ "nextBuildNumber"
        (lastNumber.text.toInt, nextNumber.text.toInt)
    }
    def post[B <: AnyRef](s : String)(f: InputStream => B) = exec(new HttpPost(s))(f)
    def postNoResult(s : String) { execNoResult(new HttpPost(s)) }
    def get[B <: AnyRef](s : String)(f: InputStream => B) = exec(new HttpGet(s))(f)
    private def exec[B <: AnyRef](method : HttpRequestBase)(f: InputStream => B) : B = {
      execute(method) match {
        case Left(code) => throw new IllegalStateException("Request failed, status code is " + code)
        case Right(r) => {
          val entity: HttpEntity = r.getEntity
          try{
             f(entity.getContent)
           }finally {
              EntityUtils.consume(entity)
           }
        }
      }
    }

    private def execNoResult(method : HttpRequestBase) {
      execute(method) match {
        case Left(code) => throw new IllegalStateException("Request failed, status code is " + code)
        case _ =>
      }
    }
    
    private def execute(method: HttpRequestBase) : Either[Int, HttpResponse] = {
      val response = specification.context match {
        case None => specification.client.execute(method)
        case Some(c) => specification.client.execute(method, c)
      }
      val code: Int = response.getStatusLine.getStatusCode
      if (code >= 400) {
        EntityUtils.consume(response.getEntity)
        Left(code)
      }
      Right(response)
    }
}
case class JenkinsBuildResult(success : Boolean, completed : Boolean, number : Int) extends BuildResult

case class JenkinsConnectSpecification(baseUrl : String, username : Option[String], password : Option[String])  {
    def this(baseUrl : String, jobName : String) {
        this(baseUrl, None, None)
    }

    def projectUrl(jobName : String) = {
        "%s/job/%s".format(baseUrl, jobName)
    }

    def jobUrl(jobName: String,  num : Int) = "%s/%s/api/xml".format(projectUrl(jobName), num)

    def jobXmlApi(jobName : String) = {
        "%s/api/xml".format(projectUrl(jobName))
    }

    def buildUrl(jobName : String) = {
        "%s/build".format(projectUrl(jobName))
    }

    def client = {
        val client = new DefaultHttpClient()
        (username, password) match {
            case (Some(u), Some(p)) => {
                val authpref = new ArrayList[String]();
                authpref.add(AuthPolicy.BASIC);
                client.getParams.setParameter(AuthPNames.PROXY_AUTH_PREF, authpref);
                val credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(u,  p))
                client.setCredentialsProvider(credsProvider)
            }
            case (_, _) =>
        }
        client.getParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
        client.getParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
        client
    }

    def context : Option[HttpContext] =
      (username, password) match {
        case (Some(_), Some(_)) => {
          val authCache = new BasicAuthCache()
          val uri = URI.create(baseUrl)
          val port = if (uri.getPort == 0) 80 else uri.getPort
          val scheme = new BasicScheme
          authCache.put(new HttpHost(uri.getHost, port), scheme)
          val context = new BasicHttpContext
          context.setAttribute(ClientContext.AUTH_CACHE, authCache)
          Some(context)
        }
        case _ => None
      }
      
}

object Specifications {
  def apply(values : Map[String, String]) =
    new BuildSpecification {
      def projectName: String = values("projectName")
    }
}