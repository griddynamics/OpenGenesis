package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.AbstractGenericRepository
import com.griddynamics.genesis.api
import com.griddynamics.genesis.model
import com.griddynamics.genesis.repository
import model.GenesisSchema
import org.squeryl.PrimitiveTypeMode._
import org.springframework.transaction.annotation.Transactional

class CredentialsRepository extends AbstractGenericRepository[model.Credentials, api.Credentials](GenesisSchema.credentials)
  with repository.CredentialsRepository {

  implicit def convert(model: model.Credentials): api.Credentials =
    new api.Credentials(fromModelId(model.id), model.projectId, model.cloudProvider, model.pairName, model.identity, model.credential)

  implicit def convert(dto: api.Credentials): model.Credentials = {
    val creds = new model.Credentials(dto.projectId, dto.cloudProvider, dto.pairName, dto.identity, dto.credential)
    creds.id = toModelId(dto.id);
    creds;
  }

  @Transactional(readOnly = true)
  def list(projectId: Int): Iterable[api.Credentials] = from(table) {
    item => where(item.projectId === projectId) select (item)
  }.map(convert(_))


  @Transactional(readOnly = true)
  def find(projectId: Int, cloudProvider: String, pairName: String): Option[api.Credentials] = from(table) {
      item =>
        where((item.projectId === projectId) and (item.cloudProvider === cloudProvider) and (item.pairName === pairName))
        select (item)
    }.headOption.map(convert(_))
}