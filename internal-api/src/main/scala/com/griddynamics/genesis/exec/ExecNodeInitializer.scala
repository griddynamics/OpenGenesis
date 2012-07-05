package com.griddynamics.genesis.exec

/*
 * Copyright (c) 2011 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   For information about the licensing and copyright of this document please
 *   contact Grid Dynamics at info@griddynamics.com.
 *
 *   $Id: $
 *   @Project:     Genesis
 *   @Description: A cloud deployment platform
 */

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
