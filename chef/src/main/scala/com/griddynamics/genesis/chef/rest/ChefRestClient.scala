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
package com.griddynamics.genesis.chef.rest

import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.api.client.{GenericType, Client}
import javax.ws.rs.core.MediaType

import java.security.PrivateKey
import com.sun.jersey.api.client.filter.LoggingFilter
import net.liftweb.json.JsonAST.{JObject, JString, JField}
import com.griddynamics.genesis.json.utils.LiftJsonClientProvider
import java.util.logging.Logger

class ChefRestClient(url: String, identity: String, privateKey: PrivateKey) {

  private val logger: Logger = Logger.getLogger(classOf[ChefRestClient].getName)

  val clientConfig = new DefaultClientConfig()
  clientConfig.getClasses.add(classOf[LiftJsonClientProvider])

  val client = Client.create(clientConfig)

  client.addFilter(new ChefMixlibAuthFilter(identity, privateKey))
  client.addFilter(new LoggingFilter(logger))

  private def jsonResource(path: String) = {
    client.resource(url + path).accept(MediaType.APPLICATION_JSON).`type`(MediaType.APPLICATION_JSON)
  }

  def listNodes(): Iterable[String] = {
    jsonResource("/nodes").get(new GenericType[Map[String, String]]() {}).keys
  }

  def deleteNode(nodeName: String) {
    jsonResource("/nodes/" + nodeName).delete()
  }

  def deleteDatabag(databag: String) {
    jsonResource("/data/" + databag).delete()
  }

  def createDatabag(databag: String) {
    jsonResource("/data/").post(Map("name" -> databag))
  }

  def listDatabags(): Iterable[String] = {
    jsonResource("/data").get(new GenericType[Map[String, String]]() {}).keys
  }


  def createDatabagItem(databag: String, item: DatabagItem) {
    jsonResource("/data/" + databag).post(JObject(JField("id", JString(item.id)) +: item.value.obj.filterNot(_.name == "id")))
  }

  def listRoles(): Iterable[String] = {
    jsonResource("/roles").get(new GenericType[Map[String, String]]() {}).keys
  }

  def deleteRole(roleName: String) {
    jsonResource("/roles/" + roleName).delete()
  }

  def createRole(role: Role) {
    jsonResource("/roles").post(role)
  }

  def listClients(): Iterable[String] = {
    jsonResource("/clients").get(new GenericType[Map[String, String]]() {}).keys
  }

  def deleteClient(clientName: String) {
    jsonResource("/clients/" + clientName).delete()
  }

  def listCookbooks(): Iterable[String] = {
    jsonResource("/cookbooks").get(new GenericType[Map[String, String]]() {}).keys
  }

  def listEnvironments(): Iterable[String] = {
    jsonResource("/environments").get(new GenericType[Map[String, String]]() {}).keys
  }
}

