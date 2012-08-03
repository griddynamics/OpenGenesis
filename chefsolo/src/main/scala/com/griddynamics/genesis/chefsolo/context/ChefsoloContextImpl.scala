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
package com.griddynamics.genesis.chefsolo.context

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.chefsolo.step.ChefsoloStepBuilderFactory
import com.griddynamics.genesis.configuration.{ComputeServiceContext, CredentialServiceContext, StoreServiceContext}
import com.griddynamics.genesis.util.InputUtil
import com.griddynamics.genesis.plugin.PluginConfigurationContext
import com.griddynamics.genesis.chefsolo.action.{PrepareNodeAction, PrepareSoloAction}
import com.griddynamics.genesis.chefsolo.executor._
import com.griddynamics.genesis.exec.action.{InitExecNode, RunPreparedExec}
import com.griddynamics.genesis.exec.{ExecNodeInitializer, ExecPluginContext, ExecResourcesImpl}
import com.griddynamics.genesis.service.{ComputeService, SshService, CredentialService}
import com.griddynamics.genesis.executors.json.PreprocessJsonActionExecutor
import com.griddynamics.genesis.actions.json.PreprocessingJsonAction

trait ChefSoloPluginContext {
    def initChefSoloExecution(action: PrepareSoloAction): InitChefSoloActionExecutor
    def prepareChefSoloExecution(action: PrepareNodeAction): PrepareNodeActionExecutor
    def preprocessJsonExecution(a: PreprocessingJsonAction): PreprocessJsonActionExecutor
    def execExecutor(a: RunPreparedExec): PreparedChefsoloExecutor
    def initExecNodeExecutor(a: InitExecNode)  :ExecNodeInitializer
    def computeService: ComputeService
}

@Configuration
class ChefsoloContextImpl {
  @Autowired var storeServiceContext: StoreServiceContext = _
  @Autowired var credentialServiceContext: CredentialServiceContext = _

  @Autowired var computeContext: ComputeServiceContext = _
  @Autowired var execPluginContext: ExecPluginContext = _
  @Autowired var pluginConfiguration: PluginConfigurationContext = _

  @Bean def execResources = new ExecResourcesImpl
  
  @Bean def chefSoloCoordinatorFactory =  new ChefsoloCoordinatorFactory({() => {
          new ChefSoloExecutionContextImpl(
            computeContext.sshService,
            credentialServiceContext.credentialService,
            execPluginContext,
            computeContext.compService)
    }})

  @Bean def chefSoloStepBuilderFactory = new ChefsoloStepBuilderFactory
}

object ChefSoloConfig {
    lazy val installResource = InputUtil.locationAsString("classpath:shell/chef-install.sh")
}

class ChefSoloExecutionContextImpl(sshService: SshService,
                                   credentialService: CredentialService,
                                   execPluginContext: ExecPluginContext,
                                   val computeService: ComputeService)  extends  ChefSoloPluginContext {

    def initChefSoloExecution(action: PrepareSoloAction) = new InitChefSoloActionExecutor(action, ChefSoloConfig.installResource, sshService)

    def prepareChefSoloExecution(action: PrepareNodeAction) = new PrepareNodeActionExecutor(action, sshService)

    def preprocessJsonExecution(action: PreprocessingJsonAction) = new PreprocessJsonActionExecutor(action, action.templatesUrl.getOrElse(""))

    def execExecutor(a: RunPreparedExec) = new PreparedChefsoloExecutor(a, sshService)

    def initExecNodeExecutor(a: InitExecNode) = execPluginContext.execNodeInitializer(a)
}
