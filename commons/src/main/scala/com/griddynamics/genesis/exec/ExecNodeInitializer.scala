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
package com.griddynamics.genesis.exec

import com.griddynamics.genesis.util.shell.command._
import com.griddynamics.genesis.util.shell.Path
import com.griddynamics.genesis.util.Logging
import ExecNodeInitializer._
import ExecRunner._
import java.lang.Boolean
import com.griddynamics.genesis.exec.action.{ExecInitSuccess, ExecInitFail, ExecResult, InitExecNode}
import com.griddynamics.genesis.service.{SshService, StoreService}
import com.griddynamics.genesis.workflow.{Signal, SyncActionExecutor}

class ExecNodeInitializer(val action: InitExecNode,
                          sshService: SshService,
                          storeService: StoreService,
                          execResources: ExecResources) extends SyncActionExecutor
with Logging {
  lazy val sshClient = sshService.sshClient(action.env, action.server)

  def startSync(): ExecResult = {
    if (!checkOrInstallAtd())
      return ExecInitFail(action)

    sshClient.exec(mkdir(genesisDir))

    // script for running and monitoring exec files
    sshClient.put(execRunSh, execResources.execRunSh)
    sshClient.exec(chmod("+x", execRunSh))

    sshClient.exec(sudo(exec("/etc/init.d/atd")("start")))

    val homeDir = sshClient.exec("cat /etc/passwd | grep `whoami` | sed 's/.*:\\([^:]*\\):[^:]*/\\1/'")
      .getOutput.takeWhile(_ != '\n')

    action.server(ExecVmAttrs.HomeDir) = homeDir
    storeService.updateServer(action.server)

    ExecInitSuccess(action)
  }

  def checkOrInstallAtd(): Boolean = {
    if (sshClient.exec(which("at")).getExitCode == 0)
      return true
    if (sshClient.exec(which("yum")).getExitCode == 0) {
      sshClient.exec(sudo(yum("-y", "install", "at")) ~ null &> null)
    } else if (sshClient.exec(which("apt-get")).getExitCode == 0) {
      sshClient.exec(sudo(`apt-get`("-y", "--force-yes", "install", "at")) ~ null &> null)
    } else {
      return false
    }

    sshClient.exec(which("at")).getExitCode == 0
  }

  def cleanUp(signal: Signal) {
    sshClient.disconnect()
  }
}

object ExecNodeInitializer {
  val genesisDir: Path = ".genesis"
}
