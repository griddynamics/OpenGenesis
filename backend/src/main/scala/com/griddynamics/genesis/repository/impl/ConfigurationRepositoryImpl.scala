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

import com.griddynamics.genesis.repository.{AbstractOrderingMapper, AbstractGenericRepository, ConfigurationRepository}
import com.griddynamics.genesis.{model, api}
import api.{Success, Failure, ExtendedResult, Configuration, Ordering}
import model.{EnvStatus, GenesisSchema}
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.annotation.RemoteGateway

@RemoteGateway("Genesis database access: ConfigurationRepository")
class ConfigurationRepositoryImpl extends AbstractGenericRepository[model.Configuration, api.Configuration](GenesisSchema.configuration) with ConfigurationRepository  {

  @Autowired var store: AttributeRepository = _

  implicit def convert(m: model.Configuration) = new api.Configuration(fromModelId(m.id), m.name, m.projectId, m.description, Map(), None, m.templateId)

  implicit def convert(dto: Configuration) = {
    val m = new model.Configuration(dto.name, dto.projectId, dto.description, dto.templateId)
    m.id = toModelId(dto.id)
    m
  }

  private[this] def listModels(projectId: Int, ordering: Option[Ordering] = None): Iterable[model.Configuration] =
    from(table) { item =>
      val orderBy = ordering.map(o => ConfigurationOrderingMapper.order(item, o)).getOrElse(item.name asc)
      where( projectId === item.projectId ) select (item) orderBy(orderBy)
    }.toList

  @Transactional(readOnly = true)
  def list(projectId: Int) = listModels(projectId).map(convert(_))

  @Transactional(readOnly = true)
  def list(projectId: Int, ordering: Ordering) =
    listModels(projectId, Option(ordering)).map(convert(_))

  @Transactional(readOnly = true)
  def get(projectId: Int, id: Int) =  {
    from(table) (
      item => where(item.id === id and item.projectId === projectId) select (item)
    ).headOption.map {convertWithAttrs(_)}
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

  @Transactional(readOnly = true)
  def getDefaultConfig(projectId: Int): ExtendedResult[api.Configuration] = {
    val configs = listModels(projectId, Option(Ordering.asc(ConfigurationOrdering.ID)))
    configs.headOption.map{ config =>
      Success(convertWithAttrs(config))
    } getOrElse {
      Failure(compoundServiceErrors = Seq(s"${configs.size} environment configurations found in project id = ${projectId}. configId parameter should be provided."))
    }
  }

  private[this] def convertWithAttrs(m: model.Configuration) =
      convert(m).copy(items = store.loadAttrs(m, GenesisSchema.configAttrs), instanceCount = Option(instanceCount(m)))

  private[this] def instanceCount(c: model.Configuration): Int = from(GenesisSchema.envs) (
    env => where( c.id === env.configurationId and c.projectId === env.projectId and not(env.status === EnvStatus.Destroyed)) compute(count())
  ).toInt

  object ConfigurationOrderingMapper extends AbstractOrderingMapper[model.Configuration] {
    import ConfigurationOrdering._

    protected def mapFieldsAstField(m: model.Configuration) = Map(
      NAME -> m.name.~, ID -> m.id
    )
  }
}

object ConfigurationOrdering {
  val NAME = "name"
  val ID = "id"
}
