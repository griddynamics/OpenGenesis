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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.{repository, api, model}
import model.GenesisSchema
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import collection.mutable.{ArrayBuffer, LinearSeq}
import java.util.UUID

class ProjectPropertyRepository extends AbstractGenericRepository[model.ProjectProperty, api.ProjectProperty](GenesisSchema.projectProperties)
  with repository.ProjectPropertyRepository {

  @Transactional(readOnly = true)
  def listForProject(projectId: Int): List[api.ProjectProperty] = {
    val modelProperties = from(GenesisSchema.projectProperties)(pp => where(pp.projectId === projectId) select(pp)).toList
    modelProperties.map(convert(_))
  }

  @Transactional
  def updateForProject(projectId: Int, properties : List[api.ProjectProperty]) {
    val modelProperties = properties.map(convert(_))

    GenesisSchema.projectProperties.deleteWhere(pp => pp.projectId === projectId)

    for (pp <- modelProperties) {
      pp.id = 0;
      pp.projectId = projectId;
    }

    if (GenesisSchema.projects.where(pp => pp.id === projectId).isEmpty) {
      val id = GenesisSchema.projects.insert(new model.Project(UUID.randomUUID.toString, Option("desc"), "manager")).id;
      for (pp <- modelProperties) {
        pp.projectId = id;
      }
    }

    GenesisSchema.projectProperties.insert(modelProperties);
  }

  override implicit def convert(entity: model.ProjectProperty): api.ProjectProperty = {
    new api.ProjectProperty(entity.id, entity.projectId, entity.name, entity.value)
  }

  override implicit def convert(dto: api.ProjectProperty): model.ProjectProperty = {
    val projectProperty = new model.ProjectProperty(dto.projectId, dto.name, dto.value)
    projectProperty.id = dto.id
    projectProperty
  }
}
