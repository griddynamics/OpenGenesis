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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.model.{VirtualMachine, Environment}
import org.jclouds.domain.Credentials
import org.jclouds.compute.domain.NodeMetadataBuilder
import org.jclouds.net.IPSocket
import com.griddynamics.genesis.util.Logging
import org.jclouds.ssh.SshClient
import com.griddynamics.genesis.jclouds.JCloudsComputeContextProvider
import service.{VmCredentialService, Credentials => GenesisCredentials, ComputeService, CredentialService}

class SshService(credentialService: CredentialService,
                 vmCredentialService: VmCredentialService,
                 computeService: ComputeService,
                 contextFactory: JCloudsComputeContextProvider) extends service.SshService with Logging {

  def sshClient(env: Environment, vm: VirtualMachine): SshClient = {
    val creds: Option[GenesisCredentials] = vmCredentialService.getCredentialsForVm(env, vm).orElse(credentialService.getCredentialsForEnvironment(env))
    val client = sshClient(vm, creds)
    client.connect()
    client
  }

  def sshClient(vm: VirtualMachine, gcredentials: Option[GenesisCredentials]): SshClient = {
    val computeContext = contextFactory.computeContext(vm);

    val credentials = gcredentials.map {
      creds => new Credentials(creds.identity, creds.credential)
    }

    val ip = computeService.getIpAddresses(vm).map(_.address).getOrElse {
      log.debug("can't get ip address of machine '%s'", vm)
      throw new IllegalArgumentException(vm.toString)
    }

    val metadata = vm.instanceId.map { computeContext.getComputeService.getNodeMetadata(_) }

    val node = credentials.map {
      creds => metadata.map { NodeMetadataBuilder.fromNodeMetadata(_).credentials(creds).build }
    }.getOrElse(metadata)

    log.debug("getting ssh client for machine with %s...", ip)
    val utils = computeContext.utils()
    val sshClient = node.map(utils.sshForNode().apply(_)) orElse
      credentials.map {
        utils.getSshClientFactory.create(new IPSocket(ip, 22), _)
      }
    sshClient.get
  }
}