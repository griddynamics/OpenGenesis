/*
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

package com.griddynamics.genesis.nexus.datasource

import com.griddynamics.genesis.template.{VarDataSource, DataSourceFactory}
import util.parsing.json.JSON
import com.griddynamics.genesis.util.IoUtil
import org.apache.commons.codec.binary.Base64
import java.net.{HttpURLConnection, URLConnection, URL}

class NexusDataSource extends VarDataSource {
    private var url: URL = _
    private var artifactProperty = "resourceURI"
    private var usernameOpt: Option[String] = None
    private var passwordOpt: Option[String] = None
    private val ServicePathDefault = "/nexus/service/local/data_index"
    
    import NexusDataSource._

    private def basicAuth(con : URLConnection) = (usernameOpt, passwordOpt) match {
        case (Some(user), Some(pass)) =>
        val encoded = Base64.encodeBase64String((user + ":" + pass).getBytes)
        con.setRequestProperty("Authorization", "Basic " + encoded)
        case _ => // no basic auth
    }

    private def request = {
        var con: URLConnection = null
        try {
            con = url.openConnection
            con.addRequestProperty("Accept", "application/json")
            basicAuth(con)
            con.connect
            IoUtil.streamAsString(con.getInputStream)
        } finally con match {
            case c: HttpURLConnection => c.disconnect
            case _ =>
        }
    }

    def getData = JSON.parseFull(request) match {
        case Some(m: Map[String, _]) => m.get(KeyData) match {
            case Some(s: Seq[_]) => s.collect {
                case artifact: Map[String, _] =>
                    artifact.getOrElse(artifactProperty, "").toString
            }
            case _ => Seq()
        }
        case _ => Seq()
    }

    def config(map: Map[String, Any])  {
        val base = map(Url).toString + map.getOrElse(ServicePath, ServicePathDefault)
        val params = ParamsGAV.flatMap(p => map.get(p.name).map(p.getParam(_))).mkString("&")
        map.get(ArtifactProperty).foreach(p => artifactProperty = String.valueOf(p))
        usernameOpt = map.get(Username).map(_.toString)
        passwordOpt = map.get(Password).map(_.toString)
        url = new URL(base + "?" +params)
    }
}

case class SearchParam(name: String, paramName: String) {
    def getParam(value: Any) = "%s=%s".format(paramName, value) 
}

object NexusDataSource {
    val Url = "url"
    val ServicePath = "servicePath"
    val KeyData = "data"
    val Username = "username"
    val Password = "password"
    val Group = SearchParam("group", "g")
    val Artifact = SearchParam("artifact", "a")
    val Version = SearchParam("version", "v")
    val Pkg = SearchParam("pkg", "p")
    val Classifier = SearchParam("classifier", "c")
    val ParamsGAV = Seq(Group, Artifact, Version, Pkg, Classifier)
    val ArtifactProperty = "artifactProperty"
}

class NexusDSFactory extends DataSourceFactory {
    val mode = "nexus"

    def newDataSource = new NexusDataSource
}
