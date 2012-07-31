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
import api.{DataItem, DataBag}
import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.repository
import model.{GenesisSchema => GS}
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean

class DatabagRepository extends AbstractGenericRepository[model.DataBag, api.DataBag](GS.dataBags)
  with repository.DatabagRepository {

  private val itemsTable = GS.dataBagItems

  implicit def convert(entity: model.DataBag) = {
    val tags: Seq[String] = if (entity.tags.trim.isEmpty) List() else List(entity.tags.trim.toLowerCase.split(" "): _*)
    new api.DataBag(fromModelId(entity.id), entity.name, tags, entity.projectId)
  }

  implicit def convert(dto: DataBag) = {
    val entity = new model.DataBag(dto.name, " " + dto.tags.distinct.mkString(" ") + " ", dto.projectId)
    entity.id = toModelId(dto.id)
    entity
  }

  def convertItem(bagId: Int, dto: api.DataItem): model.DataBagItem = {
    val entity = new model.DataBagItem(dto.name, dto.value, bagId)
    entity.id = toModelId(dto.id)
    entity
  }

  def convertItem(entity: model.DataBagItem): api.DataItem  = {
    new api.DataItem(fromModelId(entity.id), entity.itemKey, entity.itemValue, entity.dataBagId)
  }

  @Transactional(readOnly = true)
  override def get(id: Int) = {
    val bag = super.get(id)
    bag.map (_.copy(items = Some(getItems(id))))
  }

  @Transactional
  override def insert(entity: DataBag) = saveItems(super.insert(entity), entity.items)

  @Transactional
  override def update(entity: DataBag) = saveItems(super.update(entity), entity.items)

  private[this] def saveItems(bag: DataBag, items: Option[Seq[DataItem]]): DataBag = {
    val persistedItems = items.map { updateItems(bag.id.get, _) }
    bag.copy(items = persistedItems )
  }

  @Transactional(readOnly = true)
  def findByTags(tags: Seq[String], projectId: Option[Int] = None):Seq[api.DataBag] =  from(table)(bag => {
    val tags_has = { s: String => bag.tags like ("% " + s.toLowerCase + " %") }
    val alwaysTrue: BinaryOperatorNodeLogicalBoolean = 1 === 1
    where(((bag.projectId === projectId.?) or (bag.projectId isNull).inhibitWhen(projectId.isDefined)) and tags.foldLeft(alwaysTrue) { case (acc, tag) => ( tags_has (tag) and acc) } ) select (bag)
  }).toList.map(convert _)

  @Transactional(readOnly = true)
  def getItems(bagId: Int) = from(itemsTable)(item =>
    where(item.dataBagId === bagId) select (item)
  ).toList.map(convertItem _)


  @Transactional
  def deleteItem(bagId: Int, keys: List[String]) = itemsTable.deleteWhere( item =>
    (item.dataBagId === bagId) and (item.itemKey in keys)
  )

  @Transactional
  def updateItems(bagId: Int, items: Seq[api.DataItem]): Seq[api.DataItem] = {
    itemsTable.deleteWhere(item => item.dataBagId === bagId)
    val entitites: Seq[model.DataBagItem] = items.map {  it =>
      convertItem(bagId, it)
    }
    itemsTable.insert(entitites)
    items.map (_.copy(dataBagId = bagId))
  }

  @Transactional(readOnly = true)
  def findByName(name: String, projectId: Option[Int] = None): Option[DataBag] = {
    val bag = from(table)(
      bag => where(lower(bag.name) === name.toLowerCase and ((bag.projectId === projectId.?) or (bag.projectId isNull).inhibitWhen(projectId.isDefined))) select(bag)
    ).headOption
    convertWithItems(bag)
  }

  @Transactional(readOnly = true)
  def find(id: Int, projectId: Option[Int] = None): Option[DataBag] = {
    val bag = from(table)(
      bag => where(bag.id === id and ((bag.projectId === projectId.?) or (bag.projectId isNull).inhibitWhen(projectId.isDefined))) select(bag)
    ).headOption
    convertWithItems(bag)
  }

  private def convertWithItems(bag: Option[model.DataBag]): Option[DataBag] = {
    bag.map(convert _).map(it => it.copy(items = Option(getItems(it.id.get))))
  }

    override def list = {
       list(None)
    }

    def list(projectId: Option[Int]) = {
        from(table) (
            bag => where((bag.projectId === projectId.?) or (bag.projectId isNull).inhibitWhen(projectId.isDefined)) select(bag)
        ).map(convert _).toList
    }
}



