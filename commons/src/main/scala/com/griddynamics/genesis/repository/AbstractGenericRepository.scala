/*
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
package com.griddynamics.genesis.repository

import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model
import model.GenesisEntity
import org.springframework.transaction.annotation.Transactional
import org.squeryl._

abstract class AbstractGenericRepository[Model <: KeyedEntity[GenesisEntity.Id], Api](val table: Table[Model])
  extends Repository[Api]{

    implicit def convert(model: Model): Api

    implicit def convert(dto: Api): Model

    @Transactional(readOnly = true)
    def load(id: Int): Api = from(table) {
        item => where(item.id === id) select (item)
    }.single

    @Transactional(readOnly = true)
    def get(id: Int): Option[Api] = from(table) {
        item => where(item.id === id) select (item)
    }.headOption.map(convert(_))

    @Transactional(readOnly = true)
    def list: List[Api] = from(table) {
        select(_)
    }.toList.map(convert(_))

    @Transactional
    def delete(entity: Api): Int = table.deleteWhere(a => a.id === entity.id)

    @Transactional
    def delete(id: GenesisEntity.Id): Int = table.deleteWhere(a => a.id === id)

    @Transactional
    def save(entity: Api): Api = entity.id match {
        case 0 => insert(entity)
        case _ => update(entity)
    }

    @Transactional
    def insert(entity: Api): Api = table.insert(entity)

    @Transactional
    def update(entity: Api): Api = {
        table.update(entity)
        entity
    }

    protected def toModelId(id: Option[Int]): GenesisEntity.Id = id.getOrElse(0)

    protected def fromModelId(id: GenesisEntity.Id): Option[Int] = id match {
          case 0 => None
          case _ => Some(id)
        }

}
