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
package com.griddynamics.genesis.jclouds.datasource

import com.griddynamics.genesis.template.DataSourceFactory
import com.griddynamics.genesis.jclouds.JCloudsComputeContextProvider
import scala.collection.JavaConversions._
import net.sf.ehcache.CacheManager
import java.util
import org.jclouds.compute.domain.Image
import collection.immutable.ListMap
import org.jclouds.compute.ComputeService

class ImagesDataSource(provider: JCloudsComputeContextProvider, val cacheManager: CacheManager) extends CloudBaseDataSourceTemplate(provider){

  override val cacheRegion = "cloudImageDs"

  override def loadData(computeService: ComputeService) = {
    val images: util.Set[_ <: Image] = computeService.listImages()

    val nameToId = images.map { img =>
      val name: String = if (img.getName != null && !img.getName.trim.isEmpty) {
        if(img.getId.length < 10) img.getId + ": " + img.getName else img.getName
      } else {
        img.getId
      }
      (name, img.getId)
    }.toSeq

    val sorted: Seq[(String, String)] = nameToId.sortBy (_._1)
    ListMap(sorted : _*)
  }
}

class CloudImageDSFactory(provider: JCloudsComputeContextProvider, cacheManager: CacheManager) extends DataSourceFactory {
  val mode = "cloudImages"

  def newDataSource = new ImagesDataSource(provider, cacheManager)
}


