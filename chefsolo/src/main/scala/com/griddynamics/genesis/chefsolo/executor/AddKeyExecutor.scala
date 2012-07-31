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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.chefsolo.executor

import com.griddynamics.genesis.service.{CredentialService, SshService}
import com.griddynamics.genesis.workflow.SimpleSyncActionExecutor
import com.griddynamics.genesis.util.Logging
import org.jclouds.ssh.SshClient
import org.jclouds.io.payloads.FilePayload
import java.io.File
import com.griddynamics.genesis.util.shell.command.chmod
import com.griddynamics.genesis.chefsolo.action.{AddKeySuccess, AddKeyAction}

//NOTE: this executor is not used right now, but can be transformed to something else in the future
class AddKeyExecutor(override val action: AddKeyAction,
                     pubKey: String,
                     privKey: String,
                     sshService: SshService,
                     credentialService: CredentialService) extends SimpleSyncActionExecutor with Logging {

  def startSync() = {
    val sshClient = sshService.sshClient(action.env, action.server)
    var homeDir = ""
    PrepareNodeActionExecutor.sshDo(sshClient) {
      c => {
        homeDir = PrepareNodeActionExecutor.homeDir(c)
        prepareSshKey(homeDir)(c)
      }
    }
    AddKeySuccess(action)
  }

  def prepareSshKey(homeDir: String)(sshClient: SshClient) {
    sshClient.put("/tmp/key", new FilePayload(new File(pubKey)))
    val file: File = new File(privKey)
    sshClient.put(homeDir + "/" + file.getName, new FilePayload(file))
    sshClient.exec(chmod("0600", "/tmp/key"))
    log.debug("Creating ssh directory")
    sshClient.exec(install("-m", "0700", "-d", homeDir + "/.ssh"))
    log.debug("Installing key")
    sshClient.exec(mv("/tmp/key", homeDir + "/.ssh/authorized_keys"))
  }
}
