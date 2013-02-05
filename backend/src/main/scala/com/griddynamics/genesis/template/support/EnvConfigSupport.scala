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

package com.griddynamics.genesis.template.support

import com.griddynamics.genesis.api.Configuration
import com.griddynamics.genesis.template.{DataSourceFactory, VarDataSource}
import com.griddynamics.genesis.template.dsl.groovy.{Reserved, DataSourceBuilder, VariableBuilder}
import groovy.lang.Closure
import collection.JavaConversions.mapAsJavaMap
import com.griddynamics.genesis.service.EnvironmentService

object EnvConfigSupport {
  val DS_MODE = "envConfigs"

  def asGroovyMap(c: Configuration):java.util.Map[String, _] = c.items + ("instanceCount" -> c.instanceCount.getOrElse(0))

  def dsBuilder(projectId: Int, dsFactories: Seq[DataSourceFactory]) =
    new DataSourceBuilder(projectId, dsFactories.find(_.mode == DS_MODE)
      .getOrElse(throw new IllegalStateException("Env Config datasource factory must be present!")), DS_MODE)

  def fakeConfig(projectId: Int) = asGroovyMap(Configuration(Some(0), "Fake", projectId, None))
}

import EnvConfigSupport._

class EnvConfigDataSource(service: EnvironmentService) extends VarDataSource {
  private var projId: Int = _

  def getData = service.list(projId).map(c => (c.id.getOrElse(c.name).toString, c.name)).toMap

  def config(map: Map[String, Any]) {  projId = map.get("projectId").map(_.asInstanceOf[Int]).getOrElse(0)}

  override def default = service.getDefault(projId).map(_.id).get
}


class EnvConfigDataSourceFactory(service: EnvironmentService) extends DataSourceFactory {
  val mode = DS_MODE

  def newDataSource = new EnvConfigDataSource(service)
}

class EnvConfigVariableBuilder(dsClosure: Option[Closure[Unit]],
                               dsFactories: Seq[DataSourceFactory],
                               projectId: Int, defaultEnvId: Int) extends VariableBuilder(Reserved.configRef, dsClosure, dsFactories, projectId) {

  description("Environment")
  dataSource(DS_MODE)
  defaultValue(defaultEnvId)
}