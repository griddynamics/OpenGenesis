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

import com.griddynamics.genesis.plugin.RoleStep
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.util.Describer

sealed trait JCloudsStep extends Step

case class ProvisionVm(roleName: String,
                       hardwareId: Option[String],
                       imageId: Option[String],
                       quantity: Int,
                       instanceId: Option[String],
                       ip: Option[String] = None,
                       keyPair: Option[String] = None,
                       securityGroup: Option[String] = None,
                       account: scala.collection.Map[String, String] = Map()) extends JCloudsStep with RoleStep {
  def isGlobal = true

  def roles = Set(roleName)

  override val stepDescription =
    new Describer("Virtual machine(s) provisioning")
      .param("role", roleName)
      .param("hardware id", hardwareId)
      .param("image id", imageId)
      .param("quantity", quantity.toString)
      .param("instance id", instanceId)
      .param("security group", securityGroup)
      .param("ip address", ip)
      .describe
}

case class DestroyVm(roleName: String,
                     quantity: Int) extends JCloudsStep with RoleStep {
  def isGlobal = true

  def roles = Set(roleName)

  override val stepDescription = "Virtual machine(s) destruction"
}

case class DestroyEnv() extends JCloudsStep {
  override val stepDescription = "Environment destruction"
}
