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
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service
import com.griddynamics.genesis.model.{MachineStatus, BorrowedMachine, Environment}
import com.griddynamics.genesis.api.{LeaseDescription, Server}
import com.griddynamics.genesis.repository.CredentialsRepository
import java.net.InetAddress
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

class ServersLoanServiceImpl ( storeService: service.StoreService,
                               credentialsRepository: CredentialsRepository) extends service.ServersLoanService {

  @Transactional
  def loanServersToEnvironment(servers: Seq[Server], env: Environment, roleName: String, workflowId: Int, stepId: Int) = {
    for (server <- servers) yield {
      val bm = new BorrowedMachine(server.id.get, Option(server.instanceId), env.id, workflowId,
        stepId, MachineStatus.Ready, roleName, new Timestamp(System.currentTimeMillis()))
      bm.setIp(InetAddress.getByName(server.address).getHostAddress)

      server.credentialsId.foreach { credId =>
        bm.keyPair = credentialsRepository.get(env.projectId, credId).map {cred => cred.pairName }
      }
      storeService.createBM(bm)
      bm
    }
  }

  @Transactional
  def releaseServers(env: Environment, machines: Seq[BorrowedMachine]) = {
    for (machine <- machines) yield {
      machine.status = MachineStatus.Released
      machine.releaseTime = Option(new Timestamp(System.currentTimeMillis()))
      storeService.updateServer(machine)
      machine
    }
  }

  @Transactional(readOnly = true)
  def borrowedMachines(env: Environment): Seq[BorrowedMachine] = {
    storeService.listServers(env).filter(server => server.status != MachineStatus.Released)
  }

  @Transactional(readOnly = true)
  def debtorEnvironments(server: Server): Seq[LeaseDescription] = {
    val bms = storeService.findBorrowedMachinesByServerId(server.id.getOrElse(throw new IllegalArgumentException("Server id expected to be defined")))
    for (machine <- bms) yield {
      val env = storeService.findEnv(machine.envId).getOrElse(throw new IllegalStateException("Could find env id = %d ".format(machine.envId)))
      new LeaseDescription(env.id, env.name, machine.borrowTime.getTime)
    }
  }
}
