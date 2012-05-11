package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.repository.CredentialsRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.RequestResult
import com.griddynamics.genesis.api
import com.griddynamics.genesis.service
import com.griddynamics.genesis.validation.Validation._
import org.springframework.transaction.annotation.Transactional

import java.util.UUID
import com.griddynamics.genesis.util.BasicCrypto
import org.springframework.beans.factory.annotation.{Value, Autowired}
import javax.crypto.spec.SecretKeySpec
import javax.annotation.PostConstruct
;

class CredentialsStoreService(repository: CredentialsRepository) extends Validation[api.Credentials] with service.CredentialsStoreService {

  var keySpec: SecretKeySpec = _

  @Value("${genesis.hidden.secret.key:NOT_SET!!}")
  def init(key: String ) {
    keySpec = BasicCrypto.keySpec(key)
  }

  def get(id: Int) = repository.get(id)

  def delete(id: Int) = {
    repository.delete(id)
    RequestResult(isSuccess = true)
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

  protected def validateUpdate(c: api.Credentials): Option[RequestResult] = None

  protected def validateCreation(c: api.Credentials) = filterResults(Seq(
    notEmpty(c.cloudProvider, "cloudProvider"),
    notEmpty(c.pairName, "pairName"),
    notEmpty(c.identity, "identity"),
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName).isEmpty
    }
  ))
}