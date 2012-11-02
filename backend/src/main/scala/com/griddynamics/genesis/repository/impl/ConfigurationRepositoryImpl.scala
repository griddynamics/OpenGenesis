/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.{AbstractGenericRepository, ConfigurationRepository}
import com.griddynamics.genesis.{model, api}
import api.Configuration
import model.GenesisSchema
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired

class ConfigurationRepositoryImpl extends AbstractGenericRepository[model.Configuration, api.Configuration](GenesisSchema.configuration) with ConfigurationRepository  {

  @Autowired var store: AttributeRepository = _

  implicit def convert(m: model.Configuration) = new api.Configuration(fromModelId(m.id), m.name, m.projectId, m.description)

  implicit def convert(dto: Configuration) = {
    val m = new model.Configuration(dto.name, dto.projectId, dto.description)
    m.id = toModelId(dto.id)
    m
  }

  @Transactional(readOnly = true)
  def list(projectId: Int) =  from(table) (
    item => where( projectId === item.projectId ) select (item) orderBy(item.name)
  ).toList.map(convert(_))

  @Transactional(readOnly = true)
  def get(projectId: Int, id: Int) =  {
    from(table) (
      item => where(item.id === id and item.projectId === projectId) select (item)
    ).headOption.map { c =>
      convert(c).copy(items = store.loadAttrs(c, GenesisSchema.configAttrs))
    }
  }

  @Transactional
  override def save(entity: Configuration) = {
    val r = super.save(entity)
    store.setAttrs(convert(r), GenesisSchema.configAttrs, entity.items)
    r.copy(items = entity.items)
  }

  @Transactional
  override def insert(entity: Configuration) = {
    val r = super.insert(entity)
    store.setAttrs(convert(r), GenesisSchema.configAttrs, entity.items)
    r.copy(items = entity.items)
  }

  @Transactional
  def delete(projectId: Int, id: Int) = table.deleteWhere(item => (id === item.id) and (projectId === item.projectId))

  @Transactional(readOnly = true)
  def findByName(projectId: Int, name: String): Option[api.Configuration] = from(table) (
    item => where(projectId === item.projectId and name === item.name) select(item)
  ).headOption.map(convert(_))


  @Transactional
  def lookupNames(projectId: Int): Map[Int, String] = {
    from(table)(c => where(c.projectId === projectId) select(c.id, c.name)).toMap
  }
}
