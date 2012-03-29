package com.griddynamics.genesis.repository

import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model
import model.{GenesisEntity, GenesisSchema}
import org.springframework.transaction.annotation.Transactional
import org.squeryl._
import com.griddynamics.genesis.api

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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
abstract class AbstractGenericRepository[T <: KeyedEntity[GenesisEntity.Id], K] (table: Table[T]) {

  implicit def convertTo(model: T): K
  implicit def convertFrom(dto: K): T

  @Transactional(readOnly = true)
  def load(id: Int): K = from(table) {
    item => where(item.id === id) select (item)
  }.single

  @Transactional(readOnly = true)
  def list: List[K] = from(table) { select(_) }.toList.map(convertTo(_));

  @Transactional
  def delete(entity: K): Int = table.deleteWhere(a => a.id === entity.id)

  @Transactional
  def delete(id: GenesisEntity.Id): Int = table.deleteWhere(a => a.id === id)

  @Transactional
  def save(entity: K): K = entity.id match {
    case 0 => insert(entity);
    case _ => update(entity);
  };

  @Transactional
  def insert(entity: K): K = table.insert(entity)

  @Transactional
  def update(entity: K): K = {
    table.update(entity);
    entity
  }

}

class ProjectRepositoryImpl extends AbstractGenericRepository[model.Project, api.Project](GenesisSchema.projects)
  with ProjectRepository{

  override implicit def convertTo(entity: model.Project): api.Project = {
    val id = entity.id match {
      case 0 => None;
      case _ => Some(entity.id.toString);
    }
    new api.Project(id, entity.name, entity.description, entity.projectManager)
  }

  override implicit def convertFrom(dto: api.Project): model.Project = {
    val project = new model.Project(dto.name, dto.description, dto.projectManager)
    project.id = dto.id.getOrElse("0").toInt;
    project;
  }
}


