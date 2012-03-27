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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.chef

import org.springframework.core.io.Resource
import com.griddynamics.genesis.util.InputUtil
import com.griddynamics.genesis.service.Credentials
import org.jclouds.chef.ChefContextFactory
import java.util.{Collections, Properties}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.context.annotation.{Configuration, Bean}
import com.griddynamics.genesis.configuration.{StoreServiceContext, ComputeServiceContext}
import com.griddynamics.genesis.exec.ExecPluginContext
import com.griddynamics.genesis.chef.step.ChefRun
import com.griddynamics.genesis.plugin.StepExecutionContext

trait ChefPluginContext {
    def chefService: ChefService

    def chefNodeInitializer(a: action.InitChefNode): ChefNodeInitializer

    def chefRunCoordinator(s: step.ChefRun, stepContext: StepExecutionContext): ChefRunCoordinator

    def initialChefRunPreparer(a: action.PrepareInitialChefRun): ChefRunPreparer[action.PrepareInitialChefRun]

    def regularChefRunPreparer(a: action.PrepareRegularChefRun): ChefRunPreparer[action.PrepareRegularChefRun]

    def chefDatabagCreator(a: action.CreateChefDatabag): ChefDatabagCreator

    def chefRoleCreator(a: action.CreateChefRole): ChefRoleCreator

    def chefEnvDestructor(a: action.DestroyChefEnv): ChefEnvDestructor
}

@Configuration
class ChefPluginContextImpl extends ChefPluginContext {

    import ChefPluginContextImpl._

    @Autowired var computeServiceContext: ComputeServiceContext = _
    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var execPluginContext: ExecPluginContext = _

    //TODO underscores are not allowed
    @Value("${genesis.chef.id:dev}") var chefId: String = _

    @Value("${genesis.chef.identity}") var chefIdentity: String = _
    @Value("${genesis.chef.credential}") var chefCredentialResource: Resource = _
    lazy val chefCredential = InputUtil.resourceAsString(chefCredentialResource)

    @Value("${genesis.chef.validator.identity}") var chefValidatorIdentity: String = _
    @Value("${genesis.chef.validator.credential}") var chefValidatorCredentialResource: Resource = _
    lazy val chefValidatorCredential = InputUtil.resourceAsString(chefValidatorCredentialResource)

    @Value("${genesis.chef.endpoint}") var chefEndpoint: String = _

    @Bean def chefService = new ChefServiceImpl(chefId , chefEndpoint,
        new Credentials(chefValidatorIdentity, chefValidatorCredential), chefClient)

    @Bean def chefClient = {
        val chefContextFactory = new ChefContextFactory

        val overrides = new Properties
        overrides.setProperty(CHEF_ENDPOINT, chefEndpoint)

        val chefContext = chefContextFactory.createContext(chefIdentity, chefCredential, Collections.emptySet, overrides)

        chefContext.getApi
    }

    @Bean def chefResources = new ChefResourcesImpl

    @Bean def chefStepCoordinatorFactory = new ChefStepCoordinatorFactory(this)

    @Bean def chefRunStepBuilderFactory = new step.ChefRunStepBuilderFactory

    @Bean def createChefRoleBuilderFactory = new step.CreateChefRoleBuilderFactory

    @Bean def createChefDatabagBuilderFactory = new step.CreateChefDatabagBuilderFactory

    @Bean def destroyChefEnvStepBuilderFactory = new step.DestroyChefEnvStepBuilderFactory

    def chefRunCoordinator(s: ChefRun, stepContext: StepExecutionContext) =
        new ChefRunCoordinator(s, stepContext, execPluginContext, this)

    def chefNodeInitializer(a: action.InitChefNode) =
        new ChefNodeInitializer(a, computeServiceContext.sshService, chefService,
            storeServiceContext.storeService, chefResources)

    def initialChefRunPreparer(a: action.PrepareInitialChefRun) =
        new ChefRunPreparer(a, computeServiceContext.sshService, chefService) with InitialChefRun

    def regularChefRunPreparer(a: action.PrepareRegularChefRun) =
        new ChefRunPreparer(a, computeServiceContext.sshService, chefService) with RegularChefRun

    def chefRoleCreator(a: action.CreateChefRole) =
        new ChefRoleCreator(a, chefService)

    def chefDatabagCreator(a: action.CreateChefDatabag) =
        new ChefDatabagCreator(a, chefService)

    def chefEnvDestructor(a: action.DestroyChefEnv) =
        new ChefEnvDestructor(a, chefService)
}

object ChefPluginContextImpl {
    val CHEF_ENDPOINT = "chef.endpoint"
}
