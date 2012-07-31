/*
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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.users.repository

import com.griddynamics.genesis.repository.AbstractGenericRepository
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.api.UserGroup
import com.griddynamics.genesis.users.model.LocalGroup


abstract class LocalGroupRepository extends AbstractGenericRepository[LocalGroup, UserGroup](LocalUserSchema.groups)
    with UserGroupManagement {

  def search(nameLike: String): List[UserGroup] = from(table)(
    group => where(group.name like nameLike.replaceAll("\\*", "%")).select (group)
  ).toList.map(convert _)

  def findByName(name: String) : Option[UserGroup] = {
      from(table)(group => where(group.name === name).select(group)).headOption.map(convert(_))
  }

  override def update(group: UserGroup) = {
      table.update(
          g => where (g.id === group.id)
            set(
            g.name        := group.name,
            g.description := group.description,
            g.mailingList := group.mailingList
            )
      )
      group
  }

  implicit def convert(model: LocalGroup) = LocalGroupRepository.convert(model)
  implicit def convert(dto: UserGroup) = LocalGroupRepository.convert(dto)
}

object LocalGroupRepository {
  implicit def convert(model: LocalGroup) = UserGroup(model.name, model.description, model.mailingList, Some(model.id))
  implicit def convert(dto: UserGroup) = LocalGroup(dto.name, dto.description, dto.mailingList, dto.id)

}

