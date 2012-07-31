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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.jclouds.step

import reflect.BeanProperty
import com.griddynamics.genesis.plugin.{StepBuilderFactory, StepBuilder}
import java.util.{Map => JMap, Collections}
import scala.collection.JavaConversions._

class ProvisionVmsStepBuilder extends StepBuilder {
  @BeanProperty var roleName: String = _
  @BeanProperty var hardwareId: String = _
  @BeanProperty var imageId: String = _
  @BeanProperty var quantity: Int = _
  @BeanProperty var instanceId: String = _
  @BeanProperty var ip: String = _
  @BeanProperty var keyPair: String = _
  @BeanProperty var securityGroup: String = _

  @BeanProperty var account: JMap[String, String] = Collections.emptyMap()

  def getDetails = new ProvisionVm(roleName, Option(hardwareId), Option(imageId),
    if (ip == null) quantity else 1, Option(instanceId), Option(ip), Option(keyPair), Option(securityGroup), account)
}

class ProvisionVmsStepBuilderFactory extends StepBuilderFactory {
  def newStepBuilder = new ProvisionVmsStepBuilder

  val stepName = "provisionVms"
}

class DestroyEnvStepBuilderFactory extends StepBuilderFactory {
  def newStepBuilder = new StepBuilder {
    def getDetails = new DestroyEnv
  }

  val stepName = "undeployEnv"
}
