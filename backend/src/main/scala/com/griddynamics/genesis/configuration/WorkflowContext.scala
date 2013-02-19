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
package com.griddynamics.genesis.configuration

import org.springframework.beans.factory.annotation.{Value, Autowired}
import akka.actor._
import java.util.concurrent.Executors
import com.griddynamics.genesis.bean._
import org.springframework.context.annotation.{Configuration, Bean}
import com.griddynamics.genesis.plugin.{PartialStepCoordinatorFactory, CompositeStepCoordinatorFactory}
import com.griddynamics.genesis.workflow.TrivialStepExecutor
import com.griddynamics.genesis.core.TrivialStepCoordinatorFactory
import com.griddynamics.genesis.workflow.{StepResult, Step}
import com.griddynamics.genesis.service.RemoteAgentsService
import com.typesafe.config.{ConfigSyntax, ConfigParseOptions, Config, ConfigFactory}
import org.springframework.core.io.Resource

trait WorkflowContext {
    def requestBroker: RequestBroker
}

case class WorkflowConfig(beatPeriodMs: Int, flowTimeOutMs: Int, remoteExecutorWaitTimeout: Int)

@Configuration
class DefaultWorkflowContext extends WorkflowContext {
    @Value("${genesis.system.beat.period.ms:1000}") var beatPeriodMs: Int = _
    @Value("${genesis.system.flow.timeout.ms:3600000}") var flowTimeOutMs: Int = _
    @Value("${genesis.system.flow.executor.sync.threads.max:5}") var syncExecThreadPoolSize: Int = _
    @Value("${genesis.system.remote.executor.wait.timeout:10}") var remoteExecutorWaitTimeout: Int = _
    @Value("${backend.properties}") var backendProperties: Resource = _

    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var templateServiceContext: TemplateServiceContext = _
    @Autowired var remoteAgentService: RemoteAgentsService = _

    private val defaultConfigs: Config = ConfigFactory.load()
    private def overrides: Config = ConfigFactory.parseFile(backendProperties.getFile, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES))

    @Bean def actorSystem: ActorSystem = ActorSystem("genesis-actor-system", overrides.withFallback(defaultConfigs))

    @Bean def requestDispatcher: RequestDispatcher = {
      val props: TypedProps[RequestDispatcher] = TypedProps(classOf[RequestDispatcher], requestDispatcherBean)
      TypedActor(actorSystem).typedActorOf(props)
    }

    private def requestDispatcherBean: RequestDispatcher = {
      new RequestDispatcherImpl(
        WorkflowConfig(beatPeriodMs, flowTimeOutMs, remoteExecutorWaitTimeout),
        storeService = storeServiceContext.storeService,
        templateService = templateServiceContext.templateService,
        executorService = executorService,
        stepCoordinatorFactory = stepCoordinatorFactory, actorSystem = actorSystem,
        remoteAgentService = remoteAgentService)
    }

    // this executor service is used to 'asynchronously' execute SyncActionExecutors,
    // @see com.griddynamics.genesis.workflow.actor.SyncActionExecutorAdapter
    @Bean def executorService = Executors.newFixedThreadPool(syncExecThreadPoolSize)

    @Bean def requestBroker: RequestBroker = new RequestBrokerImpl(
      storeServiceContext.storeService,
      storeServiceContext.configurationRepository,
      templateServiceContext.templateService,
      requestDispatcher
    )

    @Bean def stepCoordinatorFactory = new CompositeStepCoordinatorFactory(
      stepCoordinators
      ++ executors.map(new TrivialStepCoordinatorFactory(_))
    )

    @Autowired var stepCoordinators: Array[PartialStepCoordinatorFactory] = _

    @Autowired(required = false) var executors: Array[TrivialStepExecutor[_ <: Step, _ <: StepResult]] = Array()
}