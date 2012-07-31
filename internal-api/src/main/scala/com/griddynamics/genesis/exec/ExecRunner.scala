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
package com.griddynamics.genesis.exec

import java.util.UUID
import com.griddynamics.genesis.util.shell.command._
import com.griddynamics.genesis.util.{InputUtil, Logging}
import org.jclouds.ssh.SshException
import ExecRunner._
import ExecNodeInitializer._
import com.jcraft.jsch.SftpException
import com.griddynamics.genesis.service.SshService
import com.griddynamics.genesis.workflow.{Signal, AsyncActionExecutor}
import com.griddynamics.genesis.exec.action.{ExecResult, ExecFinished, RunExec}

class ExecRunner(val action: RunExec, sshService: SshService) extends AsyncActionExecutor with Logging {
  val uuid = UUID.randomUUID.toString

  lazy val sshClient = sshService.sshClient(action.execDetails.env, action.execDetails.server)

  def startAsync() {
    sshClient.exec {
      echo(exec(execRunSh)(action.execDetails.execPath, action.execDetails.outputDir, uuid)) | sudo(at("now"))
      //nohup(execRunSh)(execDetails.execPath, execDetails.outputDir, uuid) ~ null > null < null &
    }
  }

  def getResult(): Option[ExecResult] = {
    isExecRunning() match {
      case true => None
      case false => getExecStatus() match {
        case Some(status) => Some(ExecFinished(action, Some(status)))
        case None =>
          log.debug("Failed to retrive exec status for exex details %s", action.execDetails)
          Some(ExecFinished(action, None))
      }
    }
  }

  protected  def getExecStatus(): Option[Int] = {
    try {
      Some(InputUtil.streamAsInt(sshClient.get(action.execDetails.exitStatusFile).getInput))
    } catch {
      case _: SshException => None
      case _: SftpException => None
    }
  }

  protected def isExecRunning(): Boolean = {
    val res = sshClient.exec(ps("aux") | grep(uuid)).getOutput.contains(execRunSh)

    // TODO temp fix for > output buffering
    if (!res) Thread.sleep(500)

    res
  }

  def cleanUp(signal: Signal) {
    sshClient.disconnect()
  }
}

object ExecRunner {
  val execRunSh = genesisDir / "exec-run.sh"
}
