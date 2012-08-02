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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.nexus.datasource

import com.griddynamics.genesis.template.{VarDataSource, DataSourceFactory}
import util.parsing.json.JSON
import org.apache.commons.codec.binary.Base64
import java.net.{HttpURLConnection, URLConnection, URL}
import org.apache.commons.io.FilenameUtils
import com.griddynamics.genesis.util.{Logging, IoUtil}
import com.griddynamics.genesis.service.CredentialsStoreService
import com.griddynamics.genesis.api.Credentials

class NexusDataSource(val credStore: CredentialsStoreService) extends VarDataSource with Logging {
    private var url: URL = _
    private val resourceURI = "resourceURI"
    private var credOpt: Option[Credentials] = None
    private var wildcard = "*"

    import NexusDataSource._

    private def basicAuth(con : URLConnection) = credOpt match {
        case Some(creds) =>
        val credential = credStore.decrypt(creds).credential.getOrElse("")
        val encoded = Base64.encodeBase64String((creds.identity + ":" + credential).getBytes)
        con.setRequestProperty("Authorization", "Basic " + encoded)
        con
        case _ => con// no basic auth
    }

    private def getResponse(url: URL) = {
        var con: URLConnection = null
        try {
            con = url.openConnection
            con.addRequestProperty("Accept", "application/json")
            basicAuth(con).connect
            IoUtil.streamAsString(con.getInputStream)
        } finally con match {
            case c: HttpURLConnection => c.disconnect
            case _ =>
        }
    }

    private def list(json: String) = JSON.parseFull(json) match {
        case Some(m: Map[String, _]) => m.get(KeyData) match {
            case Some(s: Seq[_]) => s.collect {
                case artifact: Map[String, _] => artifact
            }
            case _ => Seq()
        }
        case _ => Seq()
    }

    private def getData(url: URL): Seq[String] = (list(getResponse(url)) collect {
        case m if m.contains(resourceURI) =>
            val prop = m(resourceURI).toString
            m.get(KeyLeaf) match {
                case Some(false) => getData(new URL(prop))
                case _ if FilenameUtils.wildcardMatch(prop, wildcard) => Seq(prop)
                case _ => Seq()
            }

    }).flatten

    def getData = getData(url).sortBy(FilenameUtils.getName(_)).map(v => (v.substring(v.lastIndexOf("/") + 1), v)).toMap

    def config(map: Map[String, Any]) {
        val projId = map.get("projectId").map(_.asInstanceOf[Int])
        credOpt = projId.flatMap(credStore.find(_, CredProvider, map.get(Credential).getOrElse("").toString))
        url = new URL(map(Url).toString)
        wildcard = map.getOrElse(Filter, wildcard).toString
    }
}

object NexusDataSource {
    val Url = "query"
    val KeyData = "data"
    val KeyLeaf = "leaf"
    val Credential = "credential"
    val Filter = "filter"
    val CredProvider = "nexus"
}

class NexusDSFactory(val credStore: CredentialsStoreService) extends DataSourceFactory {
    val mode = "nexus"

    def newDataSource = new NexusDataSource(credStore)
}
