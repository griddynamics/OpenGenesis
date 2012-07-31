/*
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

import java.io.{FilenameFilter, File}
import io.Source
import org.apache.commons.io.FilenameUtils
import com.griddynamics.genesis.util.Logging
import org.apache.commons.codec.digest.DigestUtils


class FilesystemTemplateRepository(filesystemFolder: String, wildcard: String) extends ModeAwareTemplateRepository with Logging {
    
    var sources: Map[VersionedTemplate, String] = Map()
    var lastModifiedHash = "0"
    
    def listSources() = {
        if (sources.isEmpty || lastModifiedHash != lastModification) {
            sources = readSources()
            lastModifiedHash = lastModification
        }
        sources
    }
    
    private def readSources() = files.map(f => (VersionedTemplate(f.getAbsolutePath, f.lastModified.toString), Source.fromFile(f).getLines().mkString("\n"))).toMap


    def files: Array[File] = {
        val topDir = new File(filesystemFolder)
        topDir.listFiles(new FilenameFilter {
            def accept(dir: File, name: String) = FilenameUtils.wildcardMatch(name, wildcard, TemplateRepository.wildCardIOCase)
        })
    }

    private def lastModification: String = DigestUtils.sha256Hex(files.map(_.lastModified()).mkString(""))

    def respondTo = Modes.Local
}
