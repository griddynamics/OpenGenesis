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
package com.griddynamics.genesis.chef.executor

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.exec.ExecDetails
import com.griddynamics.genesis.util.shell.command._
import ChefNodeInitializer._
import com.griddynamics.genesis.exec.ExecNodeInitializer._
import com.griddynamics.genesis.service.{SshService, StoreService}
import com.griddynamics.genesis.chef.action.{ChefInitSuccess, InitChefNode}
import com.griddynamics.genesis.workflow.{Signal, SyncActionExecutor}
import com.griddynamics.genesis.chef.step.ChefResources
import com.griddynamics.genesis.chef.{ChefVmAttrs, ChefService}

class ChefNodeInitializer(val action: InitChefNode,
                          sshService: SshService,
                          chefService: ChefService,
                          storeService: StoreService,
                          chefResources: ChefResources) extends SyncActionExecutor
with Logging {
  val installDetails = ExecDetails(action.env, action.server, installDir / "chef-install.sh", installDir)

  lazy val sshClient = sshService.sshClient(action.env, action.server)

  def startSync() = {
    sshClient.put(validatorPem, chefService.validatorCredentials.credential)

    // script for chef install if it doesn't exist
    sshClient.exec(mkdir(installDir))
    sshClient.put(installDetails.execPath, chefResources.chefInstallSh)
    sshClient.exec(chmod("+x", installDetails.execPath))

    action.server(ChefVmAttrs.ChefNodeName) = chefService.chefClientName(action.env, action.server)
    storeService.updateServer(action.server)

    ChefInitSuccess(action, installDetails)
  }

  def cleanUp(signal: Signal) {
    sshClient.disconnect()
  }
}

object ChefNodeInitializer {
  val clientPem = genesisDir / "client.pem"
  val validatorPem = genesisDir / "validator.pem"

  val installDir = genesisDir / "chef-install"
}
