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

package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.api
import com.griddynamics.genesis.model
import com.griddynamics.genesis.repository
import model.GenesisSchema
import org.springframework.transaction.annotation.Transactional
import scala.Option
import org.squeryl.PrimitiveTypeMode._

class CredentialsRepository extends AbstractGenericRepository[model.Credentials, api.Credentials](GenesisSchema.credentials)
  with repository.CredentialsRepository {

  implicit def convert(entity: model.Credentials): api.Credentials =
    new api.Credentials(fromModelId(entity.id), entity.projectId, entity.cloudProvider, entity.pairName, entity.crIdentity, entity.credential, entity.fingerPrint)

  implicit def convert(dto: api.Credentials): model.Credentials = {
    val creds = new model.Credentials(dto.projectId, dto.cloudProvider, dto.pairName, dto.identity, dto.credential, dto.fingerPrint)
    creds.id = toModelId(dto.id)
    creds
  }

  @Transactional(readOnly = true)
  def findCredentials(projectId: Int, cloudProvider: String, fingerPrint: String): Option[api.Credentials] = from(table) (
    item => where((item.projectId === projectId) and (cloudProvider === item.cloudProvider) and (Option(fingerPrint) === item.fingerPrint) )
      select (item)
  ).headOption.map(convert(_))

  @Transactional(readOnly = true)
  def find(projectId: Int, cloudProvider: String) = from(table) (
    item => where((item.projectId === projectId) and (cloudProvider === item.cloudProvider) ) select (item)
  ).toList.map(convert(_))



  @Transactional(readOnly = true)
  def list(projectId: Int): Iterable[api.Credentials] = from(table) (
    item => where(item.projectId === projectId) select (item)
  ).map(convert(_))


  @Transactional(readOnly = true)
  def find(projectId: Int, cloudProvider: String, pairName: String): Option[api.Credentials] = from(table) (
      item =>
        where((projectId === item.projectId) and (cloudProvider === item.cloudProvider ) and (pairName === item.pairName ))
        select (item)
    ).headOption.map(convert(_))

  @Transactional(readOnly = true)
  def find(cloudProvider: String, fingerPrint: String) = from(table) (
    item =>
      where((Option(fingerPrint) === item.fingerPrint ) and (cloudProvider === item.cloudProvider) )
      select (item)
  ).headOption.map(convert(_))

  @Transactional
  def delete(projectId: Int, id: Int) = {
    table.deleteWhere(item => (id === item.id) and (projectId === item.projectId) )
  }

  @Transactional
  def get(projectId: Int, id: Int) = from(table) (
    item =>
      where((id === item.id) and (projectId === item.projectId )  )
        select (item)
  ).headOption.map(convert(_))
}