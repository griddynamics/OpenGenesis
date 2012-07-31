/**
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
package com.griddynamics.genesis.template

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.util.encoders.Base64
import org.apache.commons.io.IOCase
import com.griddynamics.genesis.api.{Tag, VcsProject}

trait TemplateRepository {
    def listSources() : Map[VersionedTemplate, String]
    /** Content for specific versioned template */

    def getContent(vt: VersionedTemplate) : Option[String] = {
      val predicate = vt.version match {
        case "LATEST" => { v: VersionedTemplate => v.name == vt.name }
        case _ => { v: VersionedTemplate => v == vt.name }
      }
      listSources().find { case (versionedTmpl, body) =>  predicate(versionedTmpl) }.map(_._2)
    }
}

trait SourceControl {
    type ChangesetType <: Any;
    def listProjects : Seq[VcsProject] = Seq[VcsProject]()
    def listTags(id : String) : Seq[Tag] = Seq[Tag]()
    def getContent(projectId: String,  tagId: String, fileName: String) : Option[String] = None
    /** Content for specific version */
    def getContentForVersion(projectId: String,  ver: ChangesetType, fileName: String) : Option[String] = None
    /** Content for latest version */
    def getContent(projectId: String,  fileName: String) : Option[(VersionedTemplate, String)] = None
    def resolve[A <: AnyRef](downloadable: A) : Option[String] = None
}

case class VersionedTemplate(name: String,  version: String = "LATEST")

object TemplateRepository {
    val wildCardIOCase = IOCase.SENSITIVE

    def revisionId(sources : Map[VersionedTemplate, String]) = {
        val digest = new SHA1Digest()

        for (hash <- sources.keys.map(v => "%s#%s".format(v.name, v.version))) {
            val bytes = hash.getBytes
            digest.update(bytes, 0, bytes.length)
        }

        val result : Array[Byte] = Array.ofDim(digest.getDigestSize)
        digest.doFinal(result, 0)

        new String(Base64.encode(result), "ASCII")
    }

    def sourcePair(source : String) = {
        val content = source.getBytes

        val digest = new SHA1Digest()
        digest.update(content, 0, content.length)

        val result = Array.ofDim[Byte](digest.getDigestSize)
        digest.doFinal(result, 0)

        (new String(Base64.encode(result), "ASCII"), source)
    }
}
