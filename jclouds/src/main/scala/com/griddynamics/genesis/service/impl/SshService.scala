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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.model._
import org.jclouds.domain.LoginCredentials
import org.jclouds.compute.domain.NodeMetadataBuilder
import com.griddynamics.genesis.util.Logging
import org.jclouds.ssh.SshClient
import com.griddynamics.genesis.jclouds.JCloudsComputeContextProvider
import service.{Credentials => GenesisCredentials, ComputeService, CredentialService}
import org.jclouds.compute.ComputeServiceContext
import java.util.Properties
import org.jclouds.ssh.jsch.config.JschSshClientModule
import java.util
import com.google.common.net.HostAndPort
import org.jclouds.ContextBuilder

class SshService(
                 credentialService: CredentialService,
                 computeService: ComputeService,
                 contextFactory: JCloudsComputeContextProvider) extends service.SshService with Logging {

  val context: ComputeServiceContext = {
    val overrides = new Properties()
    overrides.setProperty(org.jclouds.Constants.PROPERTY_ENDPOINT, "na")

    ContextBuilder
      .newBuilder("openstack-nova") // TODO: get provider(API) from configService
      .credentials("na", "na")
      .modules(util.Collections.singletonList(new JschSshClientModule))
      .overrides(overrides)
      .buildView(classOf[ComputeServiceContext])
  }

  def sshClient(env: Environment, server: EnvResource) = {
    val creds: Option[GenesisCredentials] = credentialService.getCredentials(env, server).orElse(credentialService.defaultCredentials)
    val client = sshClient(server, creds)
    client.connect()
    client
  }


  //todo this is workaround until proper CredentialService is implemented
  def sshClient(server: EnvResource, credentials: Option[service.Credentials]) = server match {
      case vm: VirtualMachine => sshClient(vm, credentials)
      case server: BorrowedMachine => sshClient(server, credentials)
  }

  private[this] def sshClient(server: BorrowedMachine, gcredentials: Option[GenesisCredentials]): SshClient = {
    val credentials = gcredentials.orElse(credentialService.defaultCredentials).map {
      credentials => LoginCredentials.builder().identity(credentials.identity).credential(credentials.credential).build()
    }
    val addresses = server.getIp.map(_.address).getOrElse(
      throw new IllegalStateException("No address provided for server %s".format(server))
    )
    context.utils().getSshClientFactory.create(HostAndPort.fromParts(addresses, 22), credentials.get)
  }


  private[this] def sshClient(vm: VirtualMachine, gcredentials: Option[GenesisCredentials]): SshClient = {
    val computeContext = contextFactory.computeContext(vm)

    val credentials = gcredentials.map {
      creds => LoginCredentials.builder().identity(creds.identity).credential(creds.credential).build()
    }

    val ip = computeService.getIpAddresses(vm).map(_.address).getOrElse {
      log.debug("can't get ip address of machine '%s'", vm)
      throw new IllegalArgumentException(vm.toString)
    }

    val metadata = vm.instanceId.map { computeContext.getComputeService.getNodeMetadata(_) }

    val node = credentials.map {
      creds => metadata.map { NodeMetadataBuilder.fromNodeMetadata(_).credentials(creds).build }
    }.getOrElse(metadata)

    if (node.isEmpty) {
      log.debug("can't get node metadata for machine '%s'", vm)
      throw new IllegalArgumentException(vm.toString)
    }

    if (node.get.getCredentials == null) {
      throw new NoCredentialsFoundException("No credentials found for node " + node.get.getId)
    }

    log.debug("getting ssh client for machine with %s...", ip)
    val utils = computeContext.utils()
    val sshClient = node.map(utils.sshForNode().apply(_)) orElse
      credentials.map {
        utils.getSshClientFactory.create(HostAndPort.fromParts(ip, 22), _)
      }
    sshClient.get
  }
}

class NoCredentialsFoundException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

