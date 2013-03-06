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

import org.apache.commons.io.FilenameUtils
import java.util.jar.JarFile
import java.net.{URL, URLClassLoader}
import java.io.File
import org.apache.commons.vfs._
import com.griddynamics.genesis.util.InputUtil

class ClassPathTemplateRepository(classLoader : ClassLoader,
                                  wildcard : String,
                                  charset : String)
    extends ClasspathStorage(classLoader, wildcard, charset) with TemplateRepository {
    import ClassPathTemplateRepository._
    def this() = this(ClassLoader.getSystemClassLoader, "*.genesis", "UTF-8")
    def listSources() = {
        files.map(sourcePair(_, charset)).toMap
    }
}

object ClassPathTemplateRepository {
    def sourcePair(file : FileObject, charset : String) = {
        val content = file.getContent.getInputStream
        val pair = TemplateRepository.sourcePair(InputUtil.streamAsString(content, charset))
        (VersionedTemplate(file.getName.getBaseName, pair._1), pair._2)
    }
}

class WildcardFileSelector(wildcard : String) extends FileSelector {
    def traverseDescendents(fileInfo : FileSelectInfo) = true

    def includeFile(fileInfo: FileSelectInfo) = {
        FilenameUtils.wildcardMatch(fileInfo.getFile.getName.getBaseName, wildcard,
                                    TemplateRepository.wildCardIOCase) &&
        fileInfo.getFile.getType == FileType.FILE
    }
}
