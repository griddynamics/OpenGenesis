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

import com.griddynamics.genesis.{model, repository, api}
import api.ServerArray
import model.{GenesisSchema => GS}
import repository.AbstractGenericRepository

import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional

class ServerArrayRepository extends AbstractGenericRepository[model.ServerArray, api.ServerArray](GS.serverArrays) with repository.ServerArrayRepository {

  val availableServerArrays = from(table, GS.projects) { (array, project) =>
    where(array.projectId === project.id and project.isDeleted === false) select(array)
  }

  implicit def convert(entity: model.ServerArray) = new api.ServerArray(fromModelId(entity.id), entity.projectId, entity.name, entity.description)

  implicit def convert(dto: ServerArray) = {
    val entity = new model.ServerArray(dto.name, dto.description, dto.projectId)
    entity.id = toModelId(dto.id)
    entity
  }

  @Transactional(readOnly = true)
  def findByName(name: String, projectId: Int) = from (availableServerArrays) (
    item =>
      where((name === item.name) and (projectId === item.projectId )  )
        select (item)
  ).headOption.map(convert(_))

  @Transactional(readOnly = true)
  def list(projectId: Int) = from(availableServerArrays) (
    item => where( projectId === item.projectId ) select (item) orderBy(item.id)
  ).toList.map(convert(_))

  @Transactional
  def delete(projectId: Int, id: Int) = {
    table.deleteWhere(item => (id === item.id) and (projectId === item.projectId) )
  }

  @Transactional
  def get(projectId: Int, id: Int) = from(availableServerArrays) (
    item =>
      where((id === item.id) and (projectId === item.projectId )  )
        select (item)
  ).headOption.map(convert(_))

}
