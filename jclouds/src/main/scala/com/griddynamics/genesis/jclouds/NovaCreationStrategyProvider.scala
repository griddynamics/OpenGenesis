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

package com.griddynamics.genesis.jclouds

import org.springframework.stereotype.Component
import com.griddynamics.genesis.model.{VirtualMachine, Environment}
import org.jclouds.compute.options.TemplateOptions
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions
import java.util.Properties

@Component
class NovaCreationStrategyProvider extends JCloudsVmCreationStrategyProvider {

  val name = "openstack-nova"

  val computeProperties = new Properties

  def createVmCreationStrategy(nodeNamePrefix: String, computeContext: org.jclouds.compute.ComputeServiceContext) =
    new DefaultVmCreationStrategy(nodeNamePrefix, computeContext) {

      override protected def templateOptions(env: Environment, vm: VirtualMachine): TemplateOptions = {
        super.templateOptions(env, vm).asInstanceOf[NovaTemplateOptions]
          .keyPairName(vm.keyPair.getOrElse(throw new IllegalArgumentException("VM keypair property should be specified")))
      }

    }
}
