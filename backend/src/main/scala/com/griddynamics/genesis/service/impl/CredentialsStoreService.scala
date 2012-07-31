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
package com.griddynamics.genesis.service.impl

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
import com.griddynamics.genesis.repository.{ProjectRepository, CredentialsRepository}

class CredentialsStoreService(repository: CredentialsRepository, projectRepository: ProjectRepository) extends Validation[api.Credentials] with service.CredentialsStoreService {

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
      RequestResult(isSuccess = false, isNotFound = true, compoundServiceErrors = List("No credentials found"))
    }
  }

  def list(projectId: Int) = repository.list(projectId)

  def find(projectId: Int, cloudProvider: String, keypairName: String): Option[api.Credentials] = repository.find(projectId, cloudProvider, keypairName)

  def findCredentials(projectId: Int, cloudProvider: String) = repository.find(projectId, cloudProvider)

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
    val credential = validatedCreds.credential.getOrElse("")
    val fingerPrint = BasicCrypto.fingerPrint(credential)
    val encrypted = BasicCrypto.encrypt(keySpec, credential)
    repository.save(validatedCreds.copy(fingerPrint = Some(fingerPrint), credential = Some(encrypted)))
  }

  protected def validateUpdate(c: api.Credentials): ExtendedResult[api.Credentials] = Failure()

  protected def validateCreation(c: api.Credentials) : ExtendedResult[api.Credentials] =
    mustSatisfyLengthConstraints(c, c.pairName, "pairName")(1, 128) ++
    mustSatisfyLengthConstraints(c, c.cloudProvider, "cloudProvider")(1, 128) ++
    mustSatisfyLengthConstraints(c, c.identity, "identity")(1, 128) ++
    (mustExist(c, "Project [id = %s] was not found".format(c.projectId)) { it => projectRepository.get(it.projectId) }) ++
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName).isEmpty
    }
}