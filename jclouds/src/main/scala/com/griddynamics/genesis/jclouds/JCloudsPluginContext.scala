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
package com.griddynamics.genesis.jclouds

import action.JCloudsProvisionVm
import coordinators.JCloudsStepCoordinatorFactory
import executors.{JCloudsVmDestructor, ProvisionExecutor, SshPortChecker}
import org.springframework.context.annotation.{Configuration, Bean}
import com.griddynamics.genesis.configuration.{StoreServiceContext, CredentialServiceContext}
import org.springframework.beans.factory.annotation.{Value, Autowired}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.{Channels, ChannelPipelineFactory}
import org.jboss.netty.handler.logging.LoggingHandler
import org.jboss.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory, InternalLogLevel}
import com.griddynamics.genesis.jclouds.step.{DestroyEnvStepBuilderFactory, ProvisionVmsStepBuilderFactory}
import com.griddynamics.genesis.workflow.DurationLimitedActionExecutor
import com.griddynamics.genesis.service.{ComputeService, impl}
import com.griddynamics.genesis.model.{IpAddresses, VirtualMachine}
import org.jclouds.compute.{ComputeServiceContextFactory, ComputeServiceContext}
import org.jclouds.Constants._
import collection.JavaConversions.{setAsJavaSet, propertiesAsScalaMap, asScalaSet}
import org.jclouds.ssh.jsch.config.JschSshClientModule
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.context.provision.ProvisionContext
import com.griddynamics.executors.provision.{CommonCheckPublicIpExecutor, CommonPortTestExecutor}
import java.util.{List => JList}
import collection.JavaConversions._

trait JCloudsPluginContext extends ProvisionContext[JCloudsProvisionVm] {
    def clientBootstrap: ClientBootstrap
    def computeContext: ComputeServiceContext
    def nodeNamePrefix: String
}

@Configuration
class JCloudsPluginContextImpl extends JCloudsPluginContext {
    @Value("${genesis.port.check.timeout.secs:180}") var portCheckTimeoutSecs: Int = _
    @Value("${genesis.provision.vm.timeout.secs:180}") var provisionVmTimeoutSecs: Int = _
    @Value("${genesis.public.ip.check.timeout.secs:30}") var publicIpCheckTimeoutSecs: Int = _

    @Value("${genesis.jclouds.provider:nova}") var jcloudsProvider: String = _
    @Value("${genesis.jclouds.endpoint:not-set}") var jcloudsEndpoint: String = _
    @Value("${genesis.jclouds.identity}") var jcloudsIdentity: String = _
    @Value("${genesis.jclouds.credential}") var jcloudsCredential: String = _
    @Value("${genesis.jclouds.nodename.prefix:GN}") var jcloudsNodeNamePrefix: String = _

    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var credentialServiceContext: CredentialServiceContext = _

    var providersMap: Map[String, JCloudsVmCreationStrategyProvider]  = _

    @Autowired
    def collectProviders(providers: JList[JCloudsVmCreationStrategyProvider]) {
      providersMap = providers.map( provider => (provider.name, provider)).toMap
    }

    lazy val currentProvider: JCloudsVmCreationStrategyProvider =
      providersMap.getOrElse(providerName, DefaultVmCreationStrategyProvider)


    @Bean def jcloudsExecutorFactory = new JCloudsStepCoordinatorFactory(this)

    @Bean def provisionVmsStepBuilderFactory = new ProvisionVmsStepBuilderFactory

    @Bean def destroyEnvStepBuilderFactory = new DestroyEnvStepBuilderFactory

    @Bean def clientBootstrap = {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        val bootstrap = new ClientBootstrap(socketChannelFactory)
        bootstrap.setPipelineFactory(channelPipelineFactory)
        bootstrap.setOption("connectTimeoutMillis", 10000)
        bootstrap
    }

    @Bean def channelPipelineFactory = {
        new ChannelPipelineFactory() {
            def getPipeline = {
                Channels.pipeline(new LoggingHandler(InternalLogLevel.INFO));
            }
        }
    }

    @Bean def socketChannelFactory = {
        new NioClientSocketChannelFactory(
            Executors.newSingleThreadExecutor(),
            Executors.newSingleThreadExecutor()
        )
    }

    @Bean def computeContext = {
        val contextFactory = new ComputeServiceContextFactory

        val overrides = currentProvider.computeProperties

        if (jcloudsEndpoint != "not-set") {
            overrides(PROPERTY_ENDPOINT) = jcloudsEndpoint
        }

        if (jcloudsEndpoint != "not-set") {
            overrides(PROPERTY_ENDPOINT) = jcloudsEndpoint
        }

        contextFactory.createContext(jcloudsProvider, jcloudsIdentity, jcloudsCredential,
            Set(new JschSshClientModule, new SLF4JLoggingModule), overrides)
    }

    @Bean def sshService = {
        new impl.SshService(credentialServiceContext.credentialService, computeService,
            computeContext)
    }

    @Bean def computeService = new JCloudsComputeService(computeContext)

    def providerName = jcloudsProvider

    def nodeNamePrefix = jcloudsNodeNamePrefix.take(2)

    @Bean def vmCreationStrategy: VmCreationStrategy =
       currentProvider.createVmCreationStrategy(nodeNamePrefix, computeContext);


    def destroyVmActionExecutor(action : DestroyVmAction) = {
        new JCloudsVmDestructor(action,
                         computeContext.getComputeService,
                         storeServiceContext.storeService)
    }

    def provisionVmActionExecutor(action: JCloudsProvisionVm) = {
        new ProvisionExecutor(action,
                              storeServiceContext.storeService,
                              vmCreationStrategy,
                              provisionVmTimeoutSecs*1000) with DurationLimitedActionExecutor
    }

    def portCheckActionExecutor(action: CheckPortAction) = {
        new CommonPortTestExecutor(action,
                             computeService,
                             storeServiceContext.storeService,
                             clientBootstrap,
                             portCheckTimeoutSecs*1000) with DurationLimitedActionExecutor
    }

    def sshPortCheckActionExecutor(action: CheckSshPortAction) = {
        new SshPortChecker(action,
                           computeService,
                           sshService,
                           storeServiceContext.storeService,
            portCheckTimeoutSecs*1000) with DurationLimitedActionExecutor
    }

    def publicIpCheckActionExecutor(action: CheckPublicIpAction) = {
        new CommonCheckPublicIpExecutor(action,
                                  computeService,
                                  storeServiceContext.storeService,
                                  publicIpCheckTimeoutSecs*1000)
    }
}

class JCloudsComputeService(computeContext: ComputeServiceContext) extends ComputeService {
        def getIpAddresses(vm: VirtualMachine) : Option[IpAddresses] = {
            vm.getIp.orElse(
            for {instanceId <- vm.instanceId
                 node = computeContext.getComputeService.getNodeMetadata(instanceId)
                 if node != null}
            yield (IpAddresses(node.getPublicAddresses.headOption, node.getPrivateAddresses.headOption)))
        }

}
