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
package com.griddynamics.genesis.jenkins.api

import org.apache.http.impl.client.{BasicAuthCache, BasicCredentialsProvider, DefaultHttpClient}
import org.apache.http.client.params.AuthPolicy
import org.apache.http.auth.params.AuthPNames
import org.apache.http.auth.{UsernamePasswordCredentials, AuthScope}
import org.apache.http.params.CoreConnectionPNames
import org.apache.http.protocol.{BasicHttpContext, HttpContext}
import java.net.URI
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.HttpHost
import org.apache.http.client.protocol.ClientContext
import java.util

case class JenkinsConnectSpecification(baseUrl: String, username: Option[String], password: Option[String]) {

  def projectUrl(jobName: String) = {
    "%s/job/%s".format(baseUrl, jobName)
  }

  def jobUrl(jobName: String, num: Int) = "%s/%s/api/xml".format(projectUrl(jobName), num)

  def jobXmlApi(jobName: String) = {
    "%s/api/xml".format(projectUrl(jobName))
  }

  def buildArtifactsJson(jobName: String) = {
    "%s/job/%s/api/json?tree=builds[number,artifacts[relativePath,fileName],url,actions[levelValue]]".format(baseUrl, jobName)
  }

  def buildUrl(jobName: String) = {
    "%s/build".format(projectUrl(jobName))
  }

  def stopBuildUrl(jobName: String, buildNumber: Int) = {
    "%s/%s/stop".format(projectUrl(jobName), buildNumber)
  }

  def client = {
    val client = new DefaultHttpClient()
    (username, password) match {
      case (Some(u), Some(p)) => {
        val authpref = new util.ArrayList[String]()
        authpref.add(AuthPolicy.BASIC)
        client.getParams.setParameter(AuthPNames.PROXY_AUTH_PREF, authpref)
        val credsProvider = new BasicCredentialsProvider()
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(u, p))
        client.setCredentialsProvider(credsProvider)
      }
      case (_, _) =>
    }
    client.getParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
    client.getParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000)
    client
  }

  def context: Option[HttpContext] =
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
