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

import com.griddynamics.genesis.jclouds.JCloudsComputeContextProvider
import net.sf.ehcache.CacheManager
import org.jclouds.compute.ComputeService
import com.griddynamics.genesis.template.DataSourceFactory
import scala.collection.JavaConversions._
import collection.immutable.ListMap


class HardwareDataSource(provider: JCloudsComputeContextProvider, val cacheManager: CacheManager) extends CloudBaseDataSourceTemplate(provider)  {

  override val cacheRegion = "cloudHardwares"

  override def loadData(computeService: ComputeService) = {
    val hardwares = computeService.listHardwareProfiles()

    val nameToId = hardwares.map { hardware =>
      val name = if (hardware.getName != null && !hardware.getName.trim.isEmpty) {
        if(hardware.getId.length < 10) hardware.getId + ": " + hardware.getName else hardware.getName
      } else {
        hardware.getId
      }

      (name, hardware.getId)
    }.toSeq

    val sorted: Seq[(String, String)] = nameToId.sortBy (_._1)
    ListMap(sorted : _*)
  }
}

class CloudHardwareDSFactory(provider: JCloudsComputeContextProvider, cacheManager: CacheManager) extends DataSourceFactory {
  val mode = "hardwareProfiles"

  def newDataSource = new HardwareDataSource(provider, cacheManager)
}
