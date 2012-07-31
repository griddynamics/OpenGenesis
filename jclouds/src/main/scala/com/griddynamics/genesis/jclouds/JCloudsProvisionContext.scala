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
package com.griddynamics.genesis.jclouds

import action.JCloudsProvisionVm
import coordinators.JCloudsStepCoordinatorFactory
import datasource.{CloudHardwareDSFactory, CloudImageDSFactory}
import executors.{JCloudsVmDestructor, ProvisionExecutor, SshPortChecker}
import com.griddynamics.genesis.jclouds.step.{DestroyEnvStepBuilderFactory, ProvisionVmsStepBuilderFactory}
import org.jclouds.Constants._
import com.griddynamics.genesis.actions.provision._
import com.griddynamics.context.provision.ProvisionContext
import com.griddynamics.executors.provision.{CommonCheckPublicIpExecutor, CommonPortTestExecutor}
import java.util.{List => JList}
import collection.JavaConversions._
import com.griddynamics.genesis.service._
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import com.griddynamics.genesis.configuration.{ClientBootstrapContext, StoreServiceContext, CredentialServiceContext}
import com.griddynamics.genesis.cache.Cache
import org.jclouds.compute.{ComputeServiceContextFactory, ComputeServiceContext}
import org.jclouds.ssh.jsch.config.JschSshClientModule
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import com.griddynamics.genesis.plugin.PluginConfigurationContext
import com.griddynamics.genesis.workflow.DurationLimitedActionExecutor
import org.springframework.context.annotation.{Configuration, Bean}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import net.sf.ehcache.CacheManager
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.model.{IpAddresses, VirtualMachine}
import org.jclouds.ec2.reference.EC2Constants

trait JCloudsProvisionContext extends ProvisionContext[JCloudsProvisionVm] {
  def cloudProvider: String
  def computeSettings: Map[String, Any]
  def storeService: StoreService
}

private object Plugin {
  val id = "jclouds"
  val Provider = "genesis.plugin.jclouds.provider"
  val Endpoint = "genesis.plugin.jclouds.endpoint"
  val Identity = "genesis.plugin.jclouds.identity"
  val Credential = "genesis.plugin.jclouds.credential"
  val NodeNamePrefix = "genesis.plugin.jclouds.nodename.prefix"

  val PortCheckoutTimeout = "genesis.plugin.jclouds.port.check.timeout.secs"
  val ProvisionTimeout = "genesis.plugin.jclouds.provision.vm.timeout.secs"
  val PublicIpCheckoutTimeout = "genesis.plugin.jclouds.public.ip.check.timeout.secs"
}

object Account {
  private val keyMap = Map("provider" -> Plugin.Provider, "endpoint" -> Plugin.Endpoint, "identity" -> Plugin.Identity, "credential" -> Plugin.Credential)
  private val keys = keyMap.keys.toSet

  def mapToComputeSettings(account: scala.collection.Map[String, String]): Map[String, String] = {
    account.collect { case(key, value) if keys.contains(key.toLowerCase) => (keyMap(key.toLowerCase), value)}.toMap
  }

  def isValid(account: scala.collection.Map[String, String]): Boolean = {
    keys.subsetOf(account.keys.map(_.toLowerCase).toSet)
  }
}

@Configuration
@GenesisPlugin(id = "jclouds", description = "Default cloud account settings")
class JCloudsPluginContextImpl extends JCloudsComputeContextProvider with Cache {

  val computeContextRegion = "computeServiceContexts"

  @Autowired var storeServiceContext: StoreServiceContext = _
  @Autowired var credentialServiceContext: CredentialServiceContext = _
  @Autowired var credentialStoreService: CredentialsStoreService = _
  @Autowired var configService: ConfigService = _
  @Autowired var pluginConfiguration: PluginConfigurationContext = _
  @Autowired var clientBootstrapContext: ClientBootstrapContext = _

  @Autowired
  var cacheManager: CacheManager = _

  @PostConstruct
  def initCache() {
    val cache = new net.sf.ehcache.Cache(
     computeContextRegion, 100, false, false, TimeUnit.HOURS.toSeconds(2), TimeUnit.HOURS.toSeconds(1), false, 0
    )
    cacheManager.addCacheIfAbsent(cache)
  }

  var providersMap: Map[String, JCloudsVmCreationStrategyProvider] = _

  @Autowired
  def collectProviders(providers: JList[JCloudsVmCreationStrategyProvider]) {
    providersMap = providers.map(provider => (provider.name, provider)).toMap
  }

  def strategyProvider(cloudProvider: String): JCloudsVmCreationStrategyProvider = {
    providersMap.getOrElse(cloudProvider, DefaultVmCreationStrategyProvider)
  }

  @Bean
  def jcloudsCoordinatorFactory = new JCloudsStepCoordinatorFactory(credentialStoreService, credentialServiceContext.credentialService, () => {
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
  })


  @Bean def provisionVmsStepBuilderFactory = new ProvisionVmsStepBuilderFactory

  @Bean def destroyEnvStepBuilderFactory = new DestroyEnvStepBuilderFactory

  @Bean def sshService: SshService = new impl.SshService(credentialServiceContext.credentialService, computeService, this)

  @Bean def computeService = new JCloudsComputeService(this)

  @Bean def cloudImageDSFactory = new CloudImageDSFactory(this, cacheManager)

  @Bean def cloudHardwareDSFactory = new CloudHardwareDSFactory(this, cacheManager)
}


class JCloudsComputeContextProvider {
  this: JCloudsPluginContextImpl =>

  import Plugin._

  def computeContext(computeSettings: Map[String, Any], specificOverrides: Option[java.util.Properties] = None): ComputeServiceContext = {
    val provider = computeSettings(Provider).toString
    val endpoint = computeSettings.get(Endpoint).map { _.toString }
    val identity = computeSettings(Identity).toString
    val credential = computeSettings(Credential).toString

    val settings = Settings(provider, endpoint, identity, credential, specificOverrides)

    fromCache(computeContextRegion, settings) {
      val contextFactory = new ComputeServiceContextFactory
      val overrides = strategyProvider(settings.provider).computeProperties.clone.asInstanceOf[java.util.Properties]
      endpoint.foreach { overrides(PROPERTY_ENDPOINT) = _ }

      for ( properties <- specificOverrides;
            property   <- properties ) {
        overrides.setProperty(property._1, property._2)
      }

      contextFactory.createContext(settings.provider, settings.identity, settings.credentials,
        Set(new JschSshClientModule, new SLF4JLoggingModule), overrides)
    }
  }

  def computeContext(vm: VirtualMachine): ComputeServiceContext = {
    val computeSettings = vm.computeSettings.getOrElse {
      throw new IllegalArgumentException("Vm [" + vm + "] wasn't created by jclouds compute plugin")
    }
    computeContext(computeSettings)
  }

  def defaultComputeSettings: Map[String, Any] = pluginConfiguration.configuration(Plugin.id)

  private case class Settings(provider: String, endpoint: Option[String], identity: String, credentials: String, props: Option[java.util.Properties])
}

class JCloudsProvisionContextImpl(override val storeService: StoreService,
                              computeService: ComputeService,
                              sshService: SshService,
                              strategies: Map[String, JCloudsVmCreationStrategyProvider],
                              bootstrapContext: ClientBootstrapContext,
                              configService: ConfigService,
                              settings: Map[String, String],
                              contextProvider: JCloudsComputeContextProvider) extends JCloudsProvisionContext {

  val provisionVmTimeout = configService.get(Plugin.ProvisionTimeout, 180) * 1000
  val portCheckTimeout = configService.get(Plugin.PortCheckoutTimeout, 180) * 1000
  val ipCheckTimeout = configService.get(Plugin.PublicIpCheckoutTimeout, 30) * 1000

  override val computeSettings = settings.filterKeys(_ != Plugin.NodeNamePrefix)

  def destroyVmActionExecutor(action : DestroyVmAction) = {
    val jcloudsComputeService = contextProvider.computeContext(action.vm).getComputeService
    new JCloudsVmDestructor(action, jcloudsComputeService, storeService)
  }

  def provisionVmActionExecutor(action: JCloudsProvisionVm) = {
    val comuteSettings = if (action.provision.isEmpty) computeSettings else action.provision
    val computeContext = contextProvider.computeContext(comuteSettings)
    val provider = comuteSettings(Plugin.Provider).toString

    val nodeNamePrefix = settings(Plugin.NodeNamePrefix).take(2)

    val vmCreationStrategy = strategies(provider).createVmCreationStrategy(nodeNamePrefix, computeContext)
    new ProvisionExecutor(action, storeService, vmCreationStrategy, provisionVmTimeout) with DurationLimitedActionExecutor
  }

  def portCheckActionExecutor(action: CheckPortAction) =
    new CommonPortTestExecutor(action, computeService, storeService, bootstrapContext.clientBootstrap, portCheckTimeout) with DurationLimitedActionExecutor

  def sshPortCheckActionExecutor(action: CheckSshPortAction) =
    new SshPortChecker(action, computeService, sshService, storeService, portCheckTimeout) with DurationLimitedActionExecutor

  def publicIpCheckActionExecutor(action: CheckPublicIpAction) =
    new CommonCheckPublicIpExecutor(action, computeService, storeService, ipCheckTimeout)

  def cloudProvider = settings(Plugin.Provider).toString
}


class JCloudsComputeService(pluginFactory: JCloudsComputeContextProvider) extends ComputeService {

  def getIpAddresses(vm: VirtualMachine): Option[IpAddresses] = {
    vm.getIp.orElse {
      val jCloudsComputeService = pluginFactory.computeContext(vm).getComputeService

      for {
        instanceId <- vm.instanceId
        node = jCloudsComputeService.getNodeMetadata(instanceId)
        if node != null
      } yield (IpAddresses(node.getPublicAddresses.headOption, node.getPrivateAddresses.headOption))
    }
  }
}