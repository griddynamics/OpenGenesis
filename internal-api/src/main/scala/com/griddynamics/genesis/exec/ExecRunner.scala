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

  lazy val sshClient = sshService.sshClient(action.execDetails.env, action.execDetails.vm)

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
