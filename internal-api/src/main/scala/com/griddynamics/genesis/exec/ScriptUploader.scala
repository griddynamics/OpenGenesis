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

import com.griddynamics.genesis.exec.action.{ScriptsUploaded, UploadScripts}
import com.griddynamics.genesis.service.SshService
import com.griddynamics.genesis.workflow.{ActionResult, Signal, SimpleSyncActionExecutor}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.logging.LoggerWrapper
import io.Source
import java.io.{IOException, BufferedInputStream}
import com.griddynamics.genesis.util.shell.command.chmod
import java.net.MalformedURLException

class ScriptUploader (val action: UploadScripts, sshService: SshService) extends SimpleSyncActionExecutor with Logging {
  lazy val sshClient = sshService.sshClient(action.env, action.server)

  override def cleanUp(signal: Signal) {
    sshClient.disconnect
  }

  private def getScriptContents(scriptName: String) = {
    try {
      val script = Source.fromInputStream(new java.net.URL(scriptName).getContent.asInstanceOf[BufferedInputStream]).getLines().mkString("\n")
      LoggerWrapper.writeLog(action.uuid, "Successfully get content of '%s'".format(scriptName))
      script
    } catch {
      case e: MalformedURLException  => {
        LoggerWrapper.writeLog(action.uuid, "Using '%s' as inline shell command".format(scriptName))
        scriptName
      }
      case e: IOException => {
        LoggerWrapper.writeLog(action.uuid, "Failed to access '%s' content: %s".format(scriptName, e.getMessage))
        throw e
      }
    }
  }

  def startSync(): ActionResult = {
    sshClient.exec("[ -d %s ] || mkdir -p %1$s".format(action.workingDir))

    val actionDir = action.workingDir +"/" + action.uuid
    sshClient.exec("[ -d %s ] || mkdir -p %1$s".format(actionDir))

    val scriptSh = actionDir + "/" + "script.sh"

    sshClient.put(scriptSh, getScriptContents(action.script))
    sshClient.exec(chmod("+x", scriptSh))

    ScriptsUploaded(action, action.server, actionDir, scriptSh)
  }
}
