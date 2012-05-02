package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.repository.CredentialsRepository
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.RequestResult
import com.griddynamics.genesis.api

class CredentialsStoreService(repository: CredentialsRepository) extends Validation[api.Credentials]{

  def get(id: Int) = repository.get(id)

  def delete(id: Int) {
    repository.delete(id)
  }

  def list(projectId: Int) = repository.list(projectId)

  def create(creds: api.Credentials) = validCreate(creds, repository.save(_))

  def update(creds: api.Credentials) = validUpdate(creds, repository.save(_))

  protected def validateUpdate(c: api.Credentials): Option[RequestResult] = filterResults(Seq(
    mustPresent(c.id, "id"),
    mustExist(c) { item => repository.get(item.id.get)},
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName).isEmpty
    }
  ))

  protected def validateCreation(c: api.Credentials) = filterResults(Seq(
    notEmpty(c.cloudProvider, "cloud provider"),
    notEmpty(c.pairName, "pair name")
  ))
}