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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.chefsolo.executor

import com.griddynamics.genesis.util.InputUtil
import com.griddynamics.genesis.model.Environment
import com.griddynamics.genesis.exec.action.RunExec
import com.griddynamics.genesis.service.{SshService, Credentials}
import org.jclouds.ssh.SshException
import com.jcraft.jsch.SftpException
import com.griddynamics.genesis.exec.ExecRunner
import com.griddynamics.genesis.chefsolo.action.ExtendedExecFinished

class PreparedChefsoloExecutor(action: RunExec, val sshService: SshService) extends ExecRunner(action, sshService){
    def env: Environment = action.execDetails.env
    def server = action.execDetails.server

    override lazy val sshClient = sshService.sshClient(env, server)

    def execStatus(): (Option[Int], Option[String], Option[String]) = {
        try {
            (Option(InputUtil.streamAsInt(sshClient.get(action.execDetails.exitStatusFile).getInput)),
              Option(InputUtil.getLines(sshClient.get(action.execDetails.stdErrFile).getInput).mkString("\n")),
              Option(InputUtil.getLines(sshClient.get(action.execDetails.stdOutFile).getInput).mkString("\n")))
        } catch {
            case _: SshException => (None, None, None)
            case _: SftpException => (None, None, None)
        }
    }

    override def getResult() = {
        isExecRunning() match {
            case true => None
            case false => execStatus() match {
                case (Some(status), errors, logs) => {
                    Some(ExtendedExecFinished(action, Some(status), errors, logs))
                }
                case (None, _, _) =>
                    log.debug("Failed to retrieve exec status for exec details %s", action.execDetails)
                    Some(ExtendedExecFinished(action, None, None, None))
            }
        }
    }
}
