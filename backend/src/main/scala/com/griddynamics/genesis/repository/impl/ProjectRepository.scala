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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.model
import model.GenesisSchema
import com.griddynamics.genesis.api
import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.repository
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

class ProjectRepository extends AbstractGenericRepository[model.Project, api.Project](GenesisSchema.projects)
  with repository.ProjectRepository {

  val activeProjects = from(table) { project =>
    where(project.isDeleted === false) select(project)
  }

  @Transactional(readOnly = true)
  override def list = from(activeProjects) { project =>
    select(project) orderBy(project.id)
  }.toList.map(convert(_))

  @Transactional(readOnly = true)
  def findByName(name: String): Option[api.Project] = from(activeProjects) {
    item => where(item.name === name) select (item)
  }.headOption.map(convert(_))

  @Transactional(readOnly = true)
  def getProjects(ids: Iterable[Int]) = from(activeProjects) {
    item => where(item.id in ids) select (item) orderBy(item.id)
  }.toList.map(convert _)

  @Transactional(readOnly = true)
  override def get(id: Int): Option[api.Project] = from(activeProjects) {
    item => where(item.id === id) select (item)
  }.headOption.map(convert(_))

  override implicit def convert(entity: model.Project): api.Project = {
    val id = entity.id match {
      case 0 => None
      case _ => Some(entity.id)
    }
    new api.Project(id, entity.name, entity.description, entity.projectManager, entity.isDeleted, entity.removalTime.map(_.getTime))
  }

  override implicit def convert(dto: api.Project): model.Project = {
    val project = new model.Project(dto.name, dto.description, dto.projectManager, dto.isDeleted, dto.removalTime.map(time => new Timestamp(time)))
    project.id = dto.id.getOrElse(0)
    project
  }
}


