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
package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.{model, api}
import com.griddynamics.genesis.repository
import com.griddynamics.genesis.repository.AbstractGenericRepository
import model.{GenesisSchema => GS}
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional


class ServerRepository extends AbstractGenericRepository[model.Server, api.Server](GS.servers) with repository.ServerRepository {

  implicit def convert(entity: model.Server) = new api.Server(fromModelId(entity.id), entity.serverArrayId, entity.instanceId, entity.address, entity.credentialsId)

  implicit def convert(dto: api.Server) = {
    val entity = new model.Server(dto.arrayId, dto.instanceId, dto.address, dto.credentialsId)
    entity.id = toModelId(dto.id)
    entity
  }

  @Transactional(readOnly = true)
  def listServers(serverArrayId: Int) = from(table)(
    item => where( serverArrayId === item.serverArrayId ) select (item) orderBy (item.id)
  ).toList.map(convert(_))


  def deleteServer (arrayId: Int, serverId: Int ): Int = {
    table.deleteWhere(item => (arrayId === item.serverArrayId) and (serverId === item.id) )
  }

  def get(arrayId: Int, serverId: Int): Option[api.Server] = {
    from(table)(item => where(item.id === serverId and item.serverArrayId === arrayId) select(item)).headOption.map(convert(_))
  }

  def findByInstanceId(arrayId: Int, instanceId: String):Option[api.Server] = from(table)(
    item => where((item.instanceId === instanceId) and (item.serverArrayId === arrayId)) select(item)
  ).headOption.map(convert(_))

}

