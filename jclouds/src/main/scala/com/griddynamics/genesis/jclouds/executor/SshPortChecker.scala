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
package com.griddynamics.genesis.jclouds.executors

import com.griddynamics.genesis.service.{ComputeService, SshService, StoreService}
import com.griddynamics.executors.provision.CommonSshPortChecker
import com.griddynamics.genesis.model.VmStatus
import com.griddynamics.genesis.workflow.{AsyncTimeoutAwareActionExecutor, Signal}
import com.griddynamics.genesis.service.impl.NoCredentialsFoundException
import com.griddynamics.genesis.actions.provision.{NoCredentialsFound, CheckSshPortAction}

class SshPortChecker(val action: CheckSshPortAction,
                     val computeService: ComputeService,
                     sshService: SshService,
                     val storeService: StoreService,
                     val timeoutMillis: Long = 180 * 1000) extends AsyncTimeoutAwareActionExecutor with CommonSshPortChecker {
  def sshClient = sshService.sshClient(action.env, action.vm)

  override def getResult() = {
    try {
      super.getResult()
    } catch {
      case e: NoCredentialsFoundException => {
        log.debug("Ssh port check was failed: %s", e.getMessage)
        action.vm.status = VmStatus.Failed
        storeService.updateServer(action.vm)
        Some(NoCredentialsFound(action, action.vm))
      }
    }
  }

   def cleanUp(signal: Signal) {}
}
