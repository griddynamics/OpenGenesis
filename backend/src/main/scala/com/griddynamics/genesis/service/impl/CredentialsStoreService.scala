package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.repository.CredentialsRepository
import com.griddynamics.genesis.validation.Validation
import com.griddynamics.genesis.api.RequestResult
import com.griddynamics.genesis.api
import com.griddynamics.genesis.service
import java.security.{DigestInputStream, MessageDigest, PrivateKey}
import java.io.ByteArrayInputStream
import org.apache.commons.codec.binary.Hex
import com.griddynamics.genesis.util.Closeables
import com.griddynamics.genesis.validation.Validation._
import scala._
import org.springframework.transaction.annotation.Transactional
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

class CredentialsStoreService(repository: CredentialsRepository) extends Validation[api.Credentials] with service.CredentialsStoreService {

  def get(id: Int) = repository.get(id)

  def delete(id: Int) = {
    repository.delete(id)
    RequestResult(isSuccess = true)
  }

  def list(projectId: Int) = repository.list(projectId)

  @Transactional
  def create(creds: api.Credentials) = {
    org.squeryl.Session.currentSession.setLogger(System.out.println _)
    validCreate(creds, validatedCreds => {
      val fingerPrint = validatedCreds.credential.map (digest (_))
      repository.save(validatedCreds.copy(fingerPrint = fingerPrint))
    })
  }

  def update(creds: api.Credentials) = {
    validUpdate(creds, validatedCreds => {
      val fingerPrint = validatedCreds.credential.map (digest (_))
      repository.save(validatedCreds.copy(fingerPrint = fingerPrint))
    })
  }

  protected def validateUpdate(c: api.Credentials): Option[RequestResult] = filterResults(Seq(
    mustPresent(c.id, "id"),
    notEmpty(c.cloudProvider, "cloud provider"),
    notEmpty(c.pairName, "key pair name"),
    mustExist(c) { item => repository.get(item.id.get) },
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName) match {
        case None => true
        case Some(cred) => cred.id == item.id
      }
    }
  ))

  protected def validateCreation(c: api.Credentials) = filterResults(Seq(
    notEmpty(c.cloudProvider, "cloudProvider"),
    notEmpty(c.pairName, "pairName"),
    notEmpty(c.identity, "identity"),
    must(c, "key pair name must be unique per provider") {
      item => repository.find(item.projectId, item.cloudProvider, item.pairName).isEmpty
    }
  ))

  def digest(k: String): String = {
    if (!k.isEmpty) {
      val md5 = MessageDigest.getInstance("SHA1");

      Closeables.using(new DigestInputStream(new ByteArrayInputStream(k.getBytes()), md5)) {
        in => while (in.read(new Array[Byte](128)) > 0) {}
      }

      val buf = new StringBuilder();
      val hex = Hex.encodeHex(md5.digest());
      for (i <- 0 until hex.length by 2) {
        if (buf.length > 0) buf.append(':');
        buf.appendAll(hex, i, 2);
      }

      buf.toString();
    } else {
      ""
    }
  }
}