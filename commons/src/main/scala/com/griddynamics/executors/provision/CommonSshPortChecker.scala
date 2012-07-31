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
package com.griddynamics.executors.provision

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.service.{StoreService, ComputeService}
import com.griddynamics.genesis.util.shell.Command._
import org.jclouds.ssh.{SshClient, SshException}
import com.griddynamics.genesis.util.shell.command.echo
import com.griddynamics.genesis.model.{VirtualMachine, VmStatus}

trait CommonSshPortChecker extends AsyncTimeoutAwareActionExecutor with Logging {
  def sshClient: SshClient
  val action : CheckSshPortAction
  def computeService : ComputeService
  def storeService : StoreService

  def startAsync() {}

  def connect : Option[(VirtualMachine, Boolean)] = {
      try {
          val client = sshClient
          client.connect()
          val isExecOk = client.exec(echo("ssh-test")).getOutput == "ssh-test\r\n"
          client.disconnect()
          Some((action.vm, isExecOk))
      } catch {
          case e : SshException => {
              log.debug("Ssh ping is failed: %s", e.getMessage)
              log.trace(e, "Ssh ping is failed trace")
              None
          }

      }
  }

  def getResult(): Option[ActionResult] = {
      connect match {
          case Some((vm, true)) => {
              vm.status = VmStatus.Ready
              storeService.updateServer(vm)
              Some(SshCheckCompleted(action, vm))
          }
          case Some((_, false)) => {
              log.debug("Exec test is failed")
              action.vm.status = VmStatus.Failed
              storeService.updateServer(action.vm)
              Some(SshCheckFailed(action, action.vm))
          }
          case None => None
      }
  }

    def getResultOnTimeout = {
        log.debug("Action timed out")
        action.vm.status = VmStatus.Failed
        storeService.updateServer(action.vm)
        SshCheckFailed(action, action.vm)
    }
}







