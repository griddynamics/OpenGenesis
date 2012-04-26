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
import com.griddynamics.genesis.jclouds.step.{DestroyEnvStepBuilderFactory, ProvisionVmsStepBuilderFactory}
import com.griddynamics.genesis.model.{IpAddresses, VirtualMachine}
import org.jclouds.Constants._
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.context.provision.ProvisionContext
import com.griddynamics.executors.provision.{CommonCheckPublicIpExecutor, CommonPortTestExecutor}
import java.util.{List => JList}
import collection.JavaConversions._
import com.griddynamics.genesis.service._
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import com.griddynamics.genesis.configuration.{ClientBootstrapContext, StoreServiceContext, CredentialServiceContext}
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.cache.Cache
import org.jclouds.compute.{ComputeServiceContextFactory, ComputeServiceContext}
import org.jclouds.ssh.jsch.config.JschSshClientModule
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import com.griddynamics.genesis.plugin.PluginConfigurationContext
import com.griddynamics.genesis.workflow.DurationLimitedActionExecutor
import com.griddynamics.genesis.util.InputUtil
import org.springframework.context.annotation.{Configuration, Bean}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ResourceLoader

trait JCloudsProvisionContext extends ProvisionContext[JCloudsProvisionVm] {
  def cloudProvider: String
  def computeSettings: Map[String, Any]
}

private object Plugin {
  val id = "jclouds"
  val Provider = "genesis.plugin.jclouds.provider"
  val Endpoint = "genesis.plugin.jclouds.endpoint"
  val Identity = "genesis.plugin.jclouds.identity"
  val Credential = "genesis.plugin.jclouds.credential"
  val NodeNamePrefix = "genesis.plugin.jclouds.nodename.prefix"

  //todo: are these global?
  val PortCheckoutTimout = "genesis.port.check.timeout.secs"
  val ProvisionTimeout = "genesis.provision.vm.timeout.secs"
  val PublicIpCheckoutTimeout = "genesis.public.ip.check.timeout.secs"
}


@Configuration
@GenesisPlugin(id = "jclouds", description = "jclouds plugin")
class JCloudsPluginContextImpl extends JCloudsComputeContextProvider with Cache {

  val computeContextRegion = "computeServiceContexts"

  @Autowired var storeServiceContext: StoreServiceContext = _
  @Autowired var credentialServiceContext: CredentialServiceContext = _
  @Autowired var configService: ConfigService = _
  @Autowired var pluginConfiguration: PluginConfigurationContext = _
  @Autowired var clientBootstrapContext: ClientBootstrapContext = _

  val cacheManager: CacheManager = CacheManager.create( this.getClass.getClassLoader.getResource("jclouds-ehcache.xml") );

  var providersMap: Map[String, JCloudsVmCreationStrategyProvider] = _

  @Autowired
  def collectProviders(providers: JList[JCloudsVmCreationStrategyProvider]) {
    providersMap = providers.map(provider => (provider.name, provider)).toMap
  }

  def strategyProvider(cloudProvider: String): JCloudsVmCreationStrategyProvider = {
    providersMap.getOrElse(cloudProvider, DefaultVmCreationStrategyProvider)
  }

  @Bean
  def jcloudsCoordinatorFactory = new JCloudsStepCoordinatorFactory(() => {
    val pluginConfig = pluginConfiguration.configuration(Plugin.id)

    new JCloudsProvisionContextImpl(
      storeServiceContext.storeService,
      computeService,
      sshService,
      providersMap,
      clientBootstrapContext,
      configService,
      pluginConfig,
      this
    )
  });


  @Bean def provisionVmsStepBuilderFactory = new ProvisionVmsStepBuilderFactory

  @Bean def destroyEnvStepBuilderFactory = new DestroyEnvStepBuilderFactory

  @Bean def sshService: SshService =
    new impl.SshService(credentialServiceContext.credentialService, stubVmCredentialService, computeService, this)

  @Bean def computeService = new JCloudsComputeService(this)


  //todo: FIXME this is temporal solution until proper CredentialService is implemented
  @Autowired var resourceLoader: ResourceLoader = _
  @Bean def stubVmCredentialService = new VmCredentialService {

    def getCredentialsForVm(vm: VirtualMachine) = vm.cloudProvider match {
      case Some(provider) => {
        for {
          identity <- configService.get("genesis.plugin.jclouds." + provider + ".vm.identity").map { _.toString }
          credential <- configService.get("genesis.plugin.jclouds." + provider + ".vm.credential").map { cred =>  resourceLoader.getResource(cred.toString) }
        } yield new Credentials(identity, InputUtil.resourceAsString(credential))
      }
      case None => None
    }

  }
}


class JCloudsComputeContextProvider {
  this: JCloudsPluginContextImpl =>

  import Plugin._

  def computeContext(computeSettings: Map[String, Any]): ComputeServiceContext = {
    val provider = computeSettings(Provider).toString
    val endpoint = computeSettings.get(Endpoint).map { _.toString }
    val identity = computeSettings(Identity).toString
    val credential = computeSettings(Credential).toString

    val settings = Settings(provider, endpoint, identity, credential)

    fromCache(computeContextRegion, settings) {
      val contextFactory = new ComputeServiceContextFactory
      val overrides = strategyProvider(settings.provider).computeProperties
      endpoint.foreach { overrides(PROPERTY_ENDPOINT) = _ }

      contextFactory.createContext(settings.provider, settings.identity, settings.credentials,
        Set(new JschSshClientModule, new SLF4JLoggingModule), overrides)
    }
  }

  def computeContext(vm: VirtualMachine): ComputeServiceContext = {
    val computeSettings = vm.getComputeSettings.getOrElse {
      throw new IllegalArgumentException("Vm [" + vm + "] wasn't created by jclouds compute plugin")
    };
    computeContext(computeSettings)
  }

  private case class Settings(provider: String, endpoint: Option[String], identity: String, credentials: String)
}

class JCloudsProvisionContextImpl(storeService: StoreService,
                              computeService: ComputeService,
                              sshService: SshService,
                              strategies: Map[String, JCloudsVmCreationStrategyProvider],
                              bootstrapContext: ClientBootstrapContext,
                              configService: ConfigService,
                              settings: Map[String, String],
                              contextProvider: JCloudsComputeContextProvider) extends JCloudsProvisionContext {

  val provisionVmTimeout = configService.get(Plugin.ProvisionTimeout, 180) * 1000
  val portCheckTimeout = configService.get(Plugin.PortCheckoutTimout, 180) * 1000
  val ipCheckTimeout = configService.get(Plugin.PublicIpCheckoutTimeout, 30) * 1000

  override val computeSettings = settings.filterKeys(_ != Plugin.NodeNamePrefix)

  def destroyVmActionExecutor(action : DestroyVmAction) = {
    val jcloudsComputeService = contextProvider.computeContext(action.vm).getComputeService
    new JCloudsVmDestructor(action, jcloudsComputeService, storeService)
  }

  def provisionVmActionExecutor(action: JCloudsProvisionVm) = {
    val computeContext = contextProvider.computeContext(computeSettings)
    val nodeNamePrefix = settings(Plugin.NodeNamePrefix).take(2)

    val vmCreationStrategy = strategies(cloudProvider).createVmCreationStrategy(nodeNamePrefix, computeContext);
    new ProvisionExecutor(action, storeService, vmCreationStrategy, provisionVmTimeout) with DurationLimitedActionExecutor
  }

  def portCheckActionExecutor(action: CheckPortAction) =
    new CommonPortTestExecutor(action, computeService, storeService, bootstrapContext.clientBootstrap, portCheckTimeout) with DurationLimitedActionExecutor

  def sshPortCheckActionExecutor(action: CheckSshPortAction) =
    new SshPortChecker(action, computeService, sshService, storeService)

  def publicIpCheckActionExecutor(action: CheckPublicIpAction) =
    new CommonCheckPublicIpExecutor(action, computeService, storeService, ipCheckTimeout)

  def cloudProvider = settings(Plugin.Provider).toString
}


class JCloudsComputeService(pluginFactory: JCloudsComputeContextProvider) extends ComputeService {

  def getIpAddresses(vm: VirtualMachine): Option[IpAddresses] = {

    val jCloudsComputeService = pluginFactory.computeContext(vm).getComputeService

    vm.getIp.orElse(
      for {
        instanceId <- vm.instanceId
        node = jCloudsComputeService.getNodeMetadata(instanceId)
        if node != null
      } yield (IpAddresses(node.getPublicAddresses.headOption, node.getPrivateAddresses.headOption))
    )
  }
}
