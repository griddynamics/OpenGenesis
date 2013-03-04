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
package com.griddynamics.genesis.build

import org.springframework.context.annotation.{Configuration, Bean}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.plugin.PluginRegistry
import scala.Array


trait BuildContext {
  def buildProvider(provider: String) : Option[BuildProvider]
}

@Configuration
class BuildPluginContextImpl {

  @Autowired var pluginRegistry: PluginRegistry = _
  // non-plugin build providers
  @Autowired var otherBuildProviders: Array[BuildProvider] = _

  @Bean def bootstrapStepBuilderFactory = new BuildStepBuilderFactory()

  @Bean def bootstrapStepCoordinatorFactory = new BuildStepCoordinatorFactory(() =>
    new BuildContext {
      val plugins = pluginRegistry.getPlugins(classOf[BuildProvider])

      def buildProvider(provider: String) =
        (plugins.values ++ otherBuildProviders).find(_.mode == provider )
    }
  )
}