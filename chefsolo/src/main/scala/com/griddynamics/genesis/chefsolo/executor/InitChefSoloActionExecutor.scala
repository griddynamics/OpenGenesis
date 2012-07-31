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

import com.griddynamics.genesis.util.shell.Path
import com.griddynamics.genesis.util.shell.Command._
import com.griddynamics.genesis.util.shell.command._
import com.griddynamics.genesis.exec.{ExecNodeInitializer, ExecDetails}
import com.griddynamics.genesis.chefsolo.action.{SoloInitSuccess, PrepareSoloAction}
import com.griddynamics.genesis.workflow.{Signal, SyncActionExecutor}
import com.griddynamics.genesis.service.{Credentials, SshService}
import org.jclouds.ssh.SshClient

class InitChefSoloActionExecutor (override val action: PrepareSoloAction, val installScript: String,
                                                             val sshService: SshService)
  extends SyncActionExecutor {

    lazy val sshClient: SshClient = sshService.sshClient(env, server)

    override def startSync() = {
        sshClient.connect()
        val homeDir = InitChefSoloActionExecutor.workingDir
        sshClient.exec(mkdir("-p", homeDir))
        sshClient.put(InitChefSoloActionExecutor.installResource, installScript)
        sshClient.exec(chmod("+x", InitChefSoloActionExecutor.installResource))
        val installDetails = ExecDetails(action.env, action.server, InitChefSoloActionExecutor.installResource, InitChefSoloActionExecutor.workingDir)
        SoloInitSuccess(action, installDetails)
    }

    def cleanUp(signal: Signal) {sshClient.disconnect()}
    def env = action.env
    def server = action.server
}

object InitChefSoloActionExecutor {
    val workingDir = ExecNodeInitializer.genesisDir / Path("chef-install")
    val installResource = workingDir / "chef-install.sh"
}
