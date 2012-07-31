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

import org.jboss.netty.bootstrap.ClientBootstrap
import com.griddynamics.genesis.util.Logging
import org.jboss.netty.channel.ChannelFuture
import java.net.InetSocketAddress
import com.griddynamics.genesis.model.VmStatus
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.service.{StoreService, ComputeService}

class CommonPortTestExecutor (val action : CheckPortAction,
                              val computeService : ComputeService,
                              val storeService : StoreService,
                              val bootstrap : ClientBootstrap,
                              val timeoutMillis : Long) extends SimpleAsyncActionExecutor
              with Logging with AsyncTimeoutAwareActionExecutor {

  var cFuture : ChannelFuture = _

  lazy val address : String = computeService.getIpAddresses(action.vm).flatMap(_.publicIp).get

  def getResult() = {
    (cFuture.isDone, cFuture.isSuccess) match {
      case (true, true) => {
        Some(PortTestCompleted(action))
      }
      case (true, false) => {
        connect()
        None
      }
      case (false, _) => {
        None
      }
    }
  }

  def startAsync() {
    connect()
  }

  override def cleanUp(signal : Signal) {
    cFuture.cancel()
    bootstrap.releaseExternalResources()
  }

  private def connect() {
    log.debug("trying to connect to host '%s', port '%s'", address, action.port)
    cFuture = bootstrap.connect(new InetSocketAddress(address, action.port));
  }

  def getResultOnTimeout = {
    action.vm.status = VmStatus.Failed
    storeService.updateServer(action.vm)
    PortTestFailed(action, action.vm)
  }
}







