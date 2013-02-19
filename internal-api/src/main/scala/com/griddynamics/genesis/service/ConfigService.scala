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

package com.griddynamics.genesis.service

import com.griddynamics.genesis.api.{ExtendedResult, ConfigProperty}


trait ConfigService {
  def get[B](name: String, default: B): B
  def get(name: String) : Option[Any]
  def getPropertyWithMeta(name: String) : Option[ConfigProperty]
  def listSettings(prefix: Option[String]) : Seq[ConfigProperty]
  def update(config: Map[String, Any]): ExtendedResult[_]
  def delete(name:String)
  def clear(prefix:Option[String])
  def get[B](projectId: Int, name: String, default: B): B
  def update(projectId: Int, config: Map[String, Any])
  def restartRequired() : Boolean
  def validateSettings : ExtendedResult[_]
  def isImportant(name: String) : Boolean
}

object GenesisSystemProperties {
  val BACKEND = "backend.properties"
  val PREFIX_GENESIS = "genesis."
  val PREFIX = "genesis.system"
  val PREFIX_DB = PREFIX + ".jdbc."
  val PLUGIN_PREFIX = "genesis.plugin"
  val PROJECT_PREFIX = "genesis.project"
  val SHUTDOWN_TIMEOUT = "genesis.system.shutdown.timeout.sec"
  val SERVICE_BACKEND_URL = "genesis.system.service.backendUrl"
  val FRONTEND_READ_TIMEOUT = "genesis.web.frontend.readTimeout.ms"
  val FRONTEND_CONNECT_TIMEOUT = "genesis.web.frontend.connectTimeout.ms"
  val SERVER_MODE = "genesis.system.server.mode"
  val SECURITY_CONFIG = "genesis.system.security.config"
  val SECURITY_GROUPS = "genesis.system.security.groups"
  val BIND_HOST = "genesis.system.bind.host"
  val BIND_PORT = "genesis.system.bind.port"
  val MAX_IDLE = "genesis.system.request.maxIdle"
  val AGENT_POLLING_PERIOD = "genesis.system.agent.poll.period"
  val WEB_RESOURCE_ROOTS = "genesis.system.web.resourceRoots"
  val CLIENT_DEBUG_MODE = "genesis.web.client.debug"
  val LOGOUT_ENABLED = "genesis.web.logout.enabled"
  val AUTH_MODE = "genesis.system.auth.mode"
  val METRICS = "genesis.system.metrics"
}