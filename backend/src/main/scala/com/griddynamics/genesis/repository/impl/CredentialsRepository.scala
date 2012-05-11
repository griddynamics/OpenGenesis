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
    new api.Credentials(fromModelId(entity.id), entity.projectId, entity.cloudProvider, entity.pairName, entity.identity, entity.credential, entity.fingerPrint)

  implicit def convert(dto: api.Credentials): model.Credentials = {
    val creds = new model.Credentials(dto.projectId, dto.cloudProvider, dto.pairName, dto.identity, dto.credential, dto.fingerPrint)
    creds.id = toModelId(dto.id);
    creds;
  }

  @Transactional(readOnly = true)
  def findCredentials(projectId: Int, cloudProvider: String, fingerPrint: String): Option[api.Credentials] = from(table) (
    item => where((item.projectId === projectId) and (cloudProvider === item.cloudProvider) and (Option(fingerPrint) === item.fingerPrint) )
      select (item)
  ).headOption.map(convert(_))

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

  def find(cloudProvider: String, fingerPrint: String) = from(table) (
    item =>
      where((Option(fingerPrint) === item.fingerPrint ) and (cloudProvider === item.cloudProvider) )
      select (item)
  ).headOption.map(convert(_))

}