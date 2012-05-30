package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.repository.CredentialsRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api
import api.{Failure, ExtendedResult, RequestResult}
import com.griddynamics.genesis.service
import com.griddynamics.genesis.validation.Validation._
import org.springframework.transaction.annotation.Transactional

import java.util.UUID
import com.griddynamics.genesis.crypto.BasicCrypto
import org.springframework.beans.factory.annotation.Value
import javax.crypto.spec.SecretKeySpec

class CredentialsStoreService(repository: CredentialsRepository) extends Validation[api.Credentials] with service.CredentialsStoreService {

  var keySpec: SecretKeySpec = _

  @Value("${genesis.hidden.secret.key:NOT_SET!!}")
  def init(key: String ) {
    keySpec = BasicCrypto.secretKeySpec(key)
  }

  def get(projectId: Int, id: Int): Option[api.Credentials] = repository.get(projectId, id)

  def delete(projectId: Int, id: Int) = {
    if(repository.delete(projectId, id) > 0){
      RequestResult(isSuccess = true)
    } else {
      RequestResult(isSuccess = false, isNotFound = true)
    }
  }

  def list(projectId: Int) = repository.list(projectId)

  def find(projectId: Int, cloudProvider: String, keypairName: String): Option[api.Credentials] = repository.find(projectId, cloudProvider, keypairName)

  def decrypt(creds: api.Credentials): api.Credentials =
    creds.copy(credential = creds.credential.map (BasicCrypto.decrypt(keySpec, _)))

  @Transactional
  def generate(projectId: Int, cloudProvider: String, identity: String, credentials: String): api.Credentials = {
    val name = "Generated-" + UUID.randomUUID().toString
    val creds = new api.Credentials(None, projectId, cloudProvider, name, identity, Some(credentials))
    saveWithFingerprints(creds)
  }

  def findCredentials(projectId: Int, cloudProvider: String, privateKey: String): Option[api.Credentials] =
    repository.find(projectId, cloudProvider, BasicCrypto.fingerPrint(privateKey))

  @Transactional
  def create(creds: api.Credentials) = validCreate(creds, saveWithFingerprints(_))

  private def saveWithFingerprints(validatedCreds: api.Credentials): api.Credentials = {
    val fingerPrint = validatedCreds.credential.map(BasicCrypto.fingerPrint(_))
    val encrypted = validatedCreds.credential.map(BasicCrypto.encrypt(keySpec, _))
    repository.save(validatedCreds.copy(fingerPrint = fingerPrint, credential = encrypted))
  }

  protected def validateUpdate(c: api.Credentials): ExtendedResult[api.Credentials] = Failure()

  protected def validateCreation(c: api.Credentials) : ExtendedResult[api.Credentials] =
    notEmpty(c, c.cloudProvider, "cloudProvider") ++
    notEmpty(c, c.pairName, "pairName") ++
    notEmpty(c, c.identity, "identity") ++
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName).isEmpty
    }
}