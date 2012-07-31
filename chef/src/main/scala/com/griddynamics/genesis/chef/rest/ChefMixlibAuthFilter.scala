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
package com.griddynamics.genesis.chef.rest

import java.security.PrivateKey
import java.text.SimpleDateFormat
import com.sun.jersey.api.client.ClientRequest
import com.sun.jersey.api.client.filter.ClientFilter
import org.apache.commons.codec.binary.Base64.encodeBase64String
import com.griddynamics.genesis.crypto.BasicCrypto
import java.util.{TimeZone, Date, Locale}
import net.liftweb.json.Serialization

class ChefMixlibAuthFilter(userIdentity: String, privateKey: PrivateKey) extends ClientFilter {
  val OpsSign = "version=1.0"
  val ChefVersion = "0.9.8"

  private val EmptyBodyHash: String = hashBody("")

  def handle(clientRequest: ClientRequest) = {
    def timeStampInUTC(): String = {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
      dateFormat.format(new Date())
    }

    val contentHash: String = hashBody(clientRequest.getEntity)
    clientRequest.getHeaders.putSingle("X-Ops-Content-Hash", contentHash)

    val timestamp = timeStampInUTC()
    val signature = signatureContent(clientRequest.getMethod, clientRequest.getURI.getPath, contentHash, timestamp)

    clientRequest.getHeaders.putSingle("X-Ops-Userid", userIdentity)
    clientRequest.getHeaders.putSingle("X-Ops-Sign", OpsSign)

    clientRequest.getHeaders.putSingle("X-Ops-Timestamp", timestamp)
    clientRequest.getHeaders.putSingle("X-Chef-Version", ChefVersion)

    signRequest(clientRequest, signature)
    getNext.handle(clientRequest)
  }


  private def signRequest(cr: ClientRequest, signature: String) {
    val signatureLines = encryptSignature(signature).grouped(60)

    val headers = cr.getHeaders

    for ((signatureLine, i) <- signatureLines.zipWithIndex) {
      headers.putSingle("X-Ops-Authorization-" + (i + 1), signatureLine)
    }
  }

  private def signatureContent(method: String, path: String, contentHash: String, timestamp: String): String = {
    "Method:" + method +
      "\nHashed Path:" + hashPath(path) +
      "\nX-Ops-Content-Hash:" + contentHash +
      "\nX-Ops-Timestamp:" + timestamp +
      "\nX-Ops-UserId:" + userIdentity
  }

  private def hashPath(path: String): String = {
    def normalizePath(path: String): String = path.replaceAll("\\/+", "/") match {
      case "/" => "/"
      case p if p.endsWith("/") => p.dropRight(1)
      case p => p
    }

    encodeBase64String(
      BasicCrypto.digestSHA1(normalizePath(path))
    )
  }


  private def hashBody(body: AnyRef): String = {
    implicit val formats = net.liftweb.json.DefaultFormats

    if (body != null) {
      val string = body match {
        case b: String => b
        case b => Serialization.write(b)
      }
      encodeBase64String(BasicCrypto.digestSHA1(string))
    } else {
      EmptyBodyHash
    }
  }

  def encryptSignature(toSign: String): String = BasicCrypto.encryptRSA(privateKey, toSign)
}
