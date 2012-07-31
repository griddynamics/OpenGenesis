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

import com.griddynamics.genesis.jclouds.{Account, JCloudsComputeContextProvider}
import com.griddynamics.genesis.template.VarDataSource
import com.griddynamics.genesis.cache.Cache
import org.jclouds.compute.ComputeService
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import org.jclouds.ec2.reference.EC2Constants._

abstract private[datasource] class CloudBaseDataSourceTemplate(provider: JCloudsComputeContextProvider) extends VarDataSource with Cache {
  def cacheRegion: String
  def loadData(computeService: ComputeService): Map[String, String]

  var filter: Option[String] = None
  var computeSettings: Map[String, Any] = _
  override def defaultTtl = TimeUnit.MINUTES.toSeconds(10).toInt

  def getData = {
    val result = fromCache(cacheRegion, computeSettings) {
      val overrides = new java.util.Properties()
      // amazon, alestic, canonical, and rightscale
      overrides.setProperty(PROPERTY_EC2_AMI_OWNERS, "137112412989,063491364108,099720109477,411009282317")

      loadData (provider.computeContext(computeSettings, Option(overrides)).getComputeService)
    }
    filter.map ( f => result.filter(_._2.matches(f)) ).getOrElse(result)
  }

  def config(map: Map[String, Any]) {
    computeSettings = map.get("account").map { case config: java.util.Map[String, String] => Account.mapToComputeSettings(config) }.getOrElse(provider.defaultComputeSettings)
    filter = map.get("filter").map(_.toString)
  }
}