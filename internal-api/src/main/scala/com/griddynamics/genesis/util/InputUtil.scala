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
package com.griddynamics.genesis.util

import java.io.{File, InputStream}
import org.springframework.core.io.{Resource, DefaultResourceLoader}
import org.apache.commons.io.IOUtils
import collection.JavaConversions

object InputUtil {

    lazy val RESOURCE_LOADER = new DefaultResourceLoader

    def streamAsInt(stream : InputStream, encoding : String = "UTF-8") =
        getLines(stream, encoding).head.toInt

    def streamAsString(stream : InputStream, encoding : String = "UTF-8") =
        getLines(stream, encoding).mkString("\n")

    def fileAsString(file : File, encoding : String = "UTF-8")  =
        streamAsString(file.toURI.toURL.openStream, encoding)

    def resourceAsString(resource : Resource, encoding : String = "UTF-8") =
        streamAsString(resource.getInputStream(), encoding)

    def locationAsString(location : String) =
        resourceAsString(RESOURCE_LOADER.getResource(location))

    def getLines(input : InputStream, encoding : String = "UTF-8") : Seq[String] = {
        try {
            val lines = IOUtils.readLines(input, encoding)
            JavaConversions.asScalaBuffer(lines).toSeq
        } finally {
            try {input.close} catch {case _ => ()}
        }
    }
}
