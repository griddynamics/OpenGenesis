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
package com.griddynamics.genesis.configuration

import com.griddynamics.genesis.plugin.api.{PluginInstanceFactory, GenesisPlugin}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.build.BuildProvider
import com.griddynamics.genesis.jenkins.build.JenkinsBuildProvider
import com.griddynamics.genesis.jenkins.api.JenkinsConnectSpecification

@GenesisPlugin(
  id = "build-jenkins",
  description = "Jenkins build step: global default settings"
)
class JenkinsBuildProviderContextImpl extends Logging with PluginInstanceFactory[BuildProvider] {

  def create(pluginConfig: Map[String, Any]): BuildProvider = {
    import Plugin._
    val baseUrl = pluginConfig(BaseUrl).toString
    val name = pluginConfig.get(UserName).map(_.toString)
    val password = pluginConfig.get(Password).map(_.toString)
    new JenkinsBuildProvider(JenkinsConnectSpecification(baseUrl, name, password))
  }

  def pluginClazz = classOf[BuildProvider]
}

private object Plugin {
  val id: String = "build-jenkins"
  val BaseUrl = "genesis.plugin.build-jenkins.baseUrl"
  val UserName = "genesis.plugin.build-jenkins.username"
  val Password = "genesis.plugin.build-jenkins.password"
}