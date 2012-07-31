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

import com.griddynamics.genesis.repository.ServerArrayRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api
import api._
import api.Failure
import api.ServerArray
import com.griddynamics.genesis.validation.Validation._
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.repository.ServerRepository
import java.net.UnknownHostException
import com.griddynamics.genesis.service.ServersService

class ServersServiceImpl(repository: ServerArrayRepository, serverRepo: ServerRepository) extends ServersService with Validation[api.ServerArray] {

  @Transactional
  def create(array: ServerArray) = validCreate(array, repository.insert(_))

  @Transactional
  def update(array: ServerArray) = validUpdate(array, repository.update(_))

  @Transactional(readOnly = true)
  def get(projectId: Int, id: Int) = repository.get(projectId, id)

  @Transactional
  def deleteServerArray(projectId: Int, id: Int) = {
    if (repository.delete(projectId, id) > 0){
      Success(None)
    } else {
      Failure(isNotFound = true, compoundServiceErrors = List("No server array was found"))
    }
  }

  @Transactional(readOnly = true)
  def list(projectId: Int) = repository.list(projectId)


  @Transactional
  def create(server: Server) = validateServer(server).map( serverRepo.insert(_) )

  @Transactional
  def deleteServer(arrayId: Int, serverId: Int) = {
    if(serverRepo.deleteServer(arrayId: Int, serverId: Int) > 0) {
      Success(None)
    } else {
      Failure(isNotFound = true, compoundServiceErrors = List("No servers found"))
    }
  }

  @Transactional
  def getServers(arrayId: Int) = serverRepo.listServers(arrayId)

  @Transactional(readOnly = true)
  def findArrayByName(projectId: Int, name: String): Option[api.ServerArray] = {
    repository.findByName(name, projectId)
  }

  @Transactional(readOnly = true)
  def getServer(arrayId: Int, serverId: Int): Option[Server] = serverRepo.get(arrayId, serverId)

  private[this] def validateServer(server: Server): ExtendedResult[Server] = {
    mustSatisfyLengthConstraints(server, server.instanceId, "instanceId")(1, 128) ++
    mustSatisfyLengthConstraints(server, server.address, "address")(1, 128) ++
    must(server, "Server with instanceId '%s' already exists".format(server.instanceId)) { server =>
      serverRepo.findByInstanceId(server.arrayId, server.instanceId).isEmpty
    } ++
    must(server, "Adress format is invalid or server is unreachable") { server =>
      try {
        java.net.InetAddress.getByName(server.address)
        true
      } catch {
        case e: UnknownHostException => false
      }
    }
  }

  protected def validateUpdate(c: ServerArray) =
    mustSatisfyLengthConstraints(c, c.name, "name")(1, 128) ++
    mustSatisfyLengthConstraints(c, c.description.getOrElse(""), "description")(0, 128) ++
    mustExist(c){ it => repository.get(it.projectId, it.id.get) } ++
    must(c, "Server array with name '" + c.name + "' already exists") {
      array => repository.findByName(array.name, array.projectId).forall { _.id == array.id}
    }


  protected def validateCreation(c: ServerArray) =
    mustSatisfyLengthConstraints(c, c.name, "name")(1, 128) ++
    mustSatisfyLengthConstraints(c, c.description.getOrElse(""), "description")(0, 128) ++
    must(c, "Server array with name '" + c.name + "' already exists") {
        array => repository.findByName(array.name, array.projectId).isEmpty
    }
}
