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
package com.griddynamics.genesis.servers

import com.griddynamics.genesis.api
import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.service.{ServersService, ServersLoanService, StoreService}
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.plugin.StepExecutionContext
import scala.Some
import scala.util.Random._

class BorrowServersActionExecutor(val action: BorrowServersAction,
                                 context: StepExecutionContext,
                                 serversService: ServersService,
                                 loanService: ServersLoanService) extends SyncActionExecutor {
  def cleanUp(signal: Signal) {}

  def startSync(): ActionResult = {
    val serverArray = {
      val opt = serversService.findArrayByName(context.env.projectId, action.serverArray)

      opt.getOrElse(return new ServerArrayNotFoundResult(action))
    }

    val available = {
      val alreadyBorrowed = loanService.borrowedMachines(context.env).map(_.serverId).toSet
      val inArray = serversService.getServers(serverArray.id.get)
      inArray.filter( server => !alreadyBorrowed.contains(server.id.get) )
    }

    val (servers, expectedSize) = action.serverIds match {
      case Some(list) => {
        ( available.filter(s => list.contains(s.instanceId) ), list.size)
      }
      case None => {
        val quantity = action.quantity.getOrElse(0)
        (shuffle(available).take(quantity), quantity)
      }
    }

    if(expectedSize != servers.size) {
      return new FailedToBorrowServersResult(action)
    }

    val borrowed = loanService.loanServersToEnvironment(servers, context.env, action.roleName, context.workflow.id, context.step.id)
    borrowed.foreach { context.updateServer(_) }

    new BorrowServerActionResult(borrowed, action)
  }

}

case class BorrowServersAction(serverArray: String,
                               roleName: String,
                               serverIds: Option[Set[String]],
                               quantity: Option[Int]) extends Action

class BorrowServerActionResult(val servers: Seq[BorrowedMachine], val action: Action) extends ActionResult  with ServersUpdateActionResult

class ServerArrayNotFoundResult(val action: BorrowServersAction) extends ActionResult with ActionFailed {
  override def desc = "Server array %s not found".format(action.serverArray)
}

class FailedToBorrowServersResult(val action: Action) extends ActionResult with ActionFailed {
  override def desc = "Failed to borrow requrested machines"
}

