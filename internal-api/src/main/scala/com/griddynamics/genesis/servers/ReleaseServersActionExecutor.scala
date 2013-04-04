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

import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.model.BorrowedMachine
import com.griddynamics.genesis.service.ServersLoanService
import com.griddynamics.genesis.plugin.{ServersUpdateResult, StepExecutionContext}
import scala.Some
import org.springframework.beans.factory.annotation.Autowired

class ReleaseServersExecutor(serversLoanService: ServersLoanService) extends TrivialStepExecutor[ReleaseServersStep, StepResult] {

  val stepType = classOf[ReleaseServersStep]
  def execute(request: ReleaseServersStep, context: StepExecutionContext) = {
    val all = context.servers.collect { case bm: BorrowedMachine => bm }

    val machines = (request.roleName, request.serverIds) match {
      case (Some(role), None) => all.filter(_.roleName == role)
      case (_, Some(seq)) => {
        val ids = seq.toSet
        all.filter { s => ids.contains(s.instanceId.get) }
      }
      case (None, None) => all
    }
    val updates = serversLoanService.releaseServers(context.env, machines)
    updates.foreach { context.updateServer(_) }

    new StepResult with ServersUpdateResult {
      val step = request

      val serversUpdate = updates
    }
  }
}

case class ReleaseServersStep( roleName: Option[String],
                               serverIds: Option[Seq[String]] ) extends Step {
  override val stepDescription = "Releasing servers borrowed from server array"
}
