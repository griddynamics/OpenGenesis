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
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.model.{VmStatus, VirtualMachine, Environment}
import com.griddynamics.genesis.workflow.{Signal, ActionResult, AsyncTimeoutAwareActionExecutor, SimpleAsyncActionExecutor}
import com.griddynamics.genesis.actions.provision.{ProvisionFailed, ProvisionCompleted, SpecificProvisionVmAction}

abstract class CommonProvisionExecutor extends SimpleAsyncActionExecutor
  with Logging with AsyncTimeoutAwareActionExecutor {
  def createVm(env: Environment, vm: VirtualMachine) : VmMetadataFuture
  def storeService: StoreService
  var vm: VirtualMachine
  val action: SpecificProvisionVmAction
  var vmMetadataFuture : VmMetadataFuture

  def startAsync() {
    vm = action.newVm
    action.ip.foreach(vm.setIp(_))
    vm = storeService.createVm(vm)
    vm.getIp.getOrElse(vmMetadataFuture = action.instanceId match {
      case None =>  {
        val future = createVm(action.env, vm)
        storeService.updateServer(vm)
        future
      }

      case some@Some(instanceId) => new VmMetadataFuture() {
        val getMetadata = some
      }
    })
  }

  def getResult(): Option[ActionResult] = {
    vm.getIp.map(_ => ProvisionCompleted(action, vm)) orElse
      (processFuture(vm))
  }

  def processFuture(vm: VirtualMachine): Option[ActionResult] = {
      vmMetadataFuture match {
          case f: FailedVmFuture => {
              vm.status = VmStatus.Failed
              storeService.updateServer(vm)
              Some(ProvisionFailed(action, Some(vm), timedOut = false))
          }
          case v: VmMetadataFuture => {
              v.getMetadata match {
                  case None => None
                  case Some(instanceId) => {
                      vm.instanceId = Some(instanceId)
                      log.debug("vm '%s' is provisioned successfuly", vm)
                      storeService.updateServer(vm)
                      Some(ProvisionCompleted(action, vm))
                  }
              }
          }
      }
  }

  def getResultOnTimeout = {
    vm.status = VmStatus.Failed
    storeService.updateServer(vm)
    ProvisionFailed(action, Some(vm), timedOut = true)
  }

  override def canRespond(s : Signal) = {
    vm.instanceId.isDefined
  }
}