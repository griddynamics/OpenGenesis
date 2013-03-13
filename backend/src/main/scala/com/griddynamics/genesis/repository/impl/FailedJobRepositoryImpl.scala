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
 */ package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.{api, model}
import com.griddynamics.genesis.model.{GenesisSchema => GS, GenesisEntity, VariablesField, FailedJobDetails}
import com.griddynamics.genesis.api.ScheduledJobDetails
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

trait FailedJobRepository {
  def list(envId: Int): Iterable[api.ScheduledJobDetails]
  def logFailure(job: api.ScheduledJobDetails): api.ScheduledJobDetails
  def delete(envId: GenesisEntity.Id, id: GenesisEntity.Id): Int
}

class FailedJobRepositoryImpl extends AbstractGenericRepository[model.FailedJobDetails, api.ScheduledJobDetails](GS.failedJobDetails) with FailedJobRepository {
  implicit def convert(model: FailedJobDetails) = {
    import VariablesField.variablesFieldToMap
    new api.ScheduledJobDetails(model.id.toString, model.projectId, model.envId, model.executionDate.getTime, model.workflow, model.variables, model.scheduledBy, Some(model.failureDescription))
  }


  implicit def convert(dto: ScheduledJobDetails) = {
    new FailedJobDetails(dto.id, dto.projectId, dto.envId, new Timestamp(dto.date), dto.workflow, dto.variables, dto.scheduledBy, dto.failureDescription.getOrElse("N/A"))
  }

  @Transactional
  def logFailure(job: ScheduledJobDetails) = insert(job)


  @Transactional
  def delete(envId: GenesisEntity.Id, id: GenesisEntity.Id) = table.deleteWhere(jd => jd.id === id and jd.envId === envId)

  @Transactional(readOnly = true)
  def list(envId: Int) = from(table) { t =>
    where(t.envId === envId) select (t)
  }.toList.map(convert)
}
