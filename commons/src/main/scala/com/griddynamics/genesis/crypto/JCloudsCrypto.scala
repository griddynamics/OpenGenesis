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
package com.griddynamics.genesis.crypto

import java.io.ByteArrayInputStream
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64.{encodeBase64String, decodeBase64}
import com.griddynamics.genesis.util.Closeables
import java.security.{PrivateKey, DigestInputStream, MessageDigest}
import org.jclouds.crypto.Pems
import org.jclouds.io.InputSuppliers
import org.jclouds.encryption.internal.JCECrypto

object JCloudsCrypto {

  val crypto: JCECrypto = new JCECrypto

  def privateKey(pem: String): PrivateKey = {
    crypto.rsaKeyFactory.generatePrivate(Pems.privateKeySpec(InputSuppliers.of(pem))) //todo(RB): dependency on jclouds
  }

  def encryptRSA(privateKey: PrivateKey, message: String): String = {
    val cipher: Cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, privateKey)
    encodeBase64String(cipher.doFinal(message.getBytes))
  }

  def digestSHA1(content: String): Array[Byte] = {
    val sha1 = MessageDigest.getInstance("SHA1")

    Closeables.using(new DigestInputStream(new ByteArrayInputStream(content.getBytes()), sha1)) {
      in => while (in.read(new Array[Byte](128)) > 0) {}
    }
    sha1.digest()
  }
}
