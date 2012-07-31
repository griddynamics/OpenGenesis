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
import com.griddynamics.genesis.model.{IpAddresses, VmStatus}
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.service.{StoreService, ComputeService}

class CommonCheckPublicIpExecutor(val action: CheckPublicIpAction,
                                  computeService: ComputeService,
                                  storeService: StoreService,
                                  val timeoutMillis : Long) extends SimpleAsyncActionExecutor with AsyncTimeoutAwareActionExecutor with Logging {
  def getResult() : Option[ActionResult] = {
    val vm = action.vm
    log.debug("Checking public ip of vm: '%s'", action.vm)
    val pubIp = computeService.getIpAddresses(action.vm).flatMap(_.publicIp)
    pubIp.foreach { ip =>
      vm.setIp(ip)
      storeService.updateServer(vm)
    }
    pubIp.map(_ => PublicIpCheckCompleted(action))
  }

  def startAsync() {}

  def getResultOnTimeout = {
    action.vm.status = VmStatus.Failed
    storeService.updateServer(action.vm)
    PublicIpCheckFailed(action, action.vm)
  }
}







