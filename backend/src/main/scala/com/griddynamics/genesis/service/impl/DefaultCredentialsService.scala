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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.model.{BorrowedMachine, EnvResource, VirtualMachine, Environment}
import com.griddynamics.genesis.service.{Credentials, CredentialService}
import com.griddynamics.genesis.service

class DefaultCredentialsService(credentialsStore: service.CredentialsStoreService, val defaultCredentials: Option[Credentials]) extends CredentialService {

  def getCredentials(env: Environment, resource: EnvResource): Option[Credentials] = {
    val provider = resource match {
      case vm: VirtualMachine => vm.cloudProvider
      case server: BorrowedMachine => Option("static")   //todo: !!!
    }

    for {
      provider <- provider
      keypair <- resource.keyPair
      creds <- credentialsStore.find(env.projectId, provider, keypair)
      credential <- credentialsStore.decrypt(creds).credential
    } yield new Credentials(creds.identity, credential)
  }

  def updateServerCredentials(env: Environment, server: EnvResource, credentials: Credentials) {
    val cloudProvider = server match {
      case vm: VirtualMachine => vm.cloudProvider.getOrElse(throw new IllegalArgumentException("cloud provider isn't set"))
      case server: BorrowedMachine => "static"   //todo: !!!
    }
    val credsOption = credentialsStore.findCredentials(env.projectId, cloudProvider, credentials.credential)
    credsOption match {
      case None => {
        val creds = credentialsStore.generate(env.projectId, cloudProvider, credentials.identity, credentials.credential)
        server.keyPair = Some(creds.pairName)
      }
      case Some(creds) => {
        server.keyPair = Some(creds.pairName)
      }
    }
  }

}