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

import coordinator.ChefStepCoordinatorFactory
import executor._
import com.griddynamics.genesis.util.InputUtil
import org.jclouds.chef.ChefContextFactory
import java.util.{Collections, Properties}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.griddynamics.genesis.configuration.{StoreServiceContext, ComputeServiceContext}
import com.griddynamics.genesis.exec.ExecPluginContext
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import com.griddynamics.genesis.service.{StoreService, SshService, Credentials}
import step.ChefResourcesImpl
import com.griddynamics.genesis.plugin.PluginConfigurationContext
import org.springframework.context.annotation.{Scope, Bean, Configuration}
import com.griddynamics.genesis.cache.Cache
import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit
import net.sf.ehcache.CacheManager

trait ChefPluginContext {

    def chefNodeInitializer(a: action.InitChefNode): ChefNodeInitializer

    def initialChefRunPreparer(a: action.PrepareInitialChefRun): ChefRunPreparer[action.PrepareInitialChefRun]

    def regularChefRunPreparer(a: action.PrepareRegularChefRun): ChefRunPreparer[action.PrepareRegularChefRun]

    def chefDatabagCreator(a: action.CreateChefDatabag): ChefDatabagCreator

    def chefRoleCreator(a: action.CreateChefRole): ChefRoleCreator

    def chefEnvDestructor(a: action.DestroyChefEnv): ChefEnvDestructor
}

//private object Plugin {
//  val id = "chef"
//
//  val ChefId = "genesis.plugin.chef.id"
//  val Identity = "genesis.plugin.chef.identity"
//  val Credential = "genesis.plugin.chef.credential"
//  val ValidatorIdentity = "genesis.plugin.chef.validator.identity"
//  val ValidatorCredential = "genesis.plugin.chef.validator.credential"
//  val Endpoint = "genesis.plugin.chef.endpoint"
//
//  val ChefInstallScript = "genesis.plugin.chef.install.sh"
//}

//@Bean
//@Scope(value = "prototype")
//case class ChefPluginConfig @Autowired() (
//  @Value("${genesis.plugin.chef.id}") chefId: String,
//  @Value("${genesis.plugin.chef.identity}") chefIdentity: String,
//  @Value("${genesis.plugin.chef.credential}") chefCredentialResource: Resource,
//  @Value("${genesis.plugin.chef.validator.identity}") chefValidatorIdentity: String,
//  @Value("${genesis.plugin.chef.validator.credential}") chefValidatorCredentialResource: Resource,
//  @Value("${genesis.plugin.chef.endpoint}") chefEndpoint: String,
//  @Value("${genesis.plugin.shell.chef.install.sh:classpath:shell/chef-install.sh}") chefInstallShResource : Resource)
//{
//
//  @transient lazy val chefCredential = InputUtil.resourceAsString(chefCredentialResource)
//  @transient lazy val chefValidatorCredential = InputUtil.resourceAsString(chefValidatorCredentialResource)
//  @transient lazy val chefResources = new ChefResourcesImpl(chefInstallShResource)
//}

@Configuration
@GenesisPlugin(id="chef", description = "Chef plugin")
class ChefPluginContextImpl extends Cache /*extends ApplicationContextAware*/ {

    import ChefPluginContextImpl._

    @Autowired var computeServiceContext: ComputeServiceContext = _
    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var execPluginContext: ExecPluginContext = _

    @Autowired var pluginConfiguration: PluginConfigurationContext = _

    @Autowired var cacheManager: CacheManager = _

    @Bean @Scope(value = "prototype")
    def config: ChefPluginConfig  = new ChefPluginConfig

    @PostConstruct
    def cacheRegionAdjustment() {
      val cache = cacheManager.addCacheIfAbsent(ChefPluginContextImpl.CacheRegion)
      val config = cache.getCacheConfiguration;
      config.setTimeToIdleSeconds(TimeUnit.HOURS.toSeconds(2))
      config.setTimeToLiveSeconds(TimeUnit.HOURS.toSeconds(20))
      config.setDiskPersistent(false)
    }

    def chefService(config: ChefPluginConfig) = new ChefServiceImpl(config.chefId , config.chefEndpoint,
        new Credentials(config.chefValidatorIdentity, config.chefValidatorCredential), chefClient(config))

    def chefClient(config: ChefPluginConfig) = {
      fromCache(ChefPluginContextImpl.CacheRegion, config) {

        val chefContextFactory = new ChefContextFactory

        val overrides = new Properties
        overrides.setProperty(CHEF_ENDPOINT, config.chefEndpoint)

        val chefContext = chefContextFactory.createContext(config.chefIdentity, config.chefCredential, Collections.emptySet, overrides)

        chefContext.getApi
      }
    }

   @Bean def chefStepCoordinatorFactory =
    new ChefStepCoordinatorFactory(execPluginContext,
      (value: Option[ChefPluginConfig]) => {
//        val configSnapshot = applicationContext.getBean(classOf[ChefPluginConfig])
        val configSnapshot = value.getOrElse(config)

        new ChefExecutionContextImpl(
          computeServiceContext.sshService,
          chefService(configSnapshot),
          storeServiceContext.storeService,
          configSnapshot
        )
      }
    )

  @Bean def chefRunStepBuilderFactory = new step.ChefRunStepBuilderFactory
  @Bean def createChefRoleBuilderFactory = new step.CreateChefRoleBuilderFactory
  @Bean def createChefDatabagBuilderFactory = new step.CreateChefDatabagBuilderFactory
  @Bean def destroyChefEnvStepBuilderFactory = new step.DestroyChefEnvStepBuilderFactory
}


class ChefPluginConfig extends Serializable {
  //TODO underscores are not allowed
  @Value("${genesis.plugin.chef.id:dev}") var chefId: String = _

  @Value("${genesis.plugin.chef.endpoint}") var chefEndpoint: String = _

  @Value("${genesis.plugin.chef.identity}") var chefIdentity: String = _
  @Value("${genesis.plugin.chef.credential}") var chefCredentialResource: String = _

  @Value("${genesis.plugin.chef.validator.identity}") var chefValidatorIdentity: String = _
  @Value("${genesis.plugin.chef.validator.credential}") var chefValidatorCredentialResource: String = _

  @Value("${genesis.plugin.shell.chef.install.sh:classpath:shell/chef-install.sh}")
  var chefInstallShResource : String = _

  @transient lazy val chefCredential = InputUtil.locationAsString(chefCredentialResource)
  @transient lazy val chefValidatorCredential = InputUtil.locationAsString(chefValidatorCredentialResource)
  @transient lazy val chefResources = new ChefResourcesImpl(chefInstallShResource)
}


class ChefExecutionContextImpl(sshService: SshService,
                               chefService: ChefService,
                               storeService: StoreService,
                               config: ChefPluginConfig)
  extends ChefPluginContext {

  def chefNodeInitializer(a: action.InitChefNode) =
      new ChefNodeInitializer(a, sshService, chefService, storeService, config)

  def initialChefRunPreparer(a: action.PrepareInitialChefRun) =
      new ChefRunPreparer(a, sshService, chefService) with InitialChefRun

  def regularChefRunPreparer(a: action.PrepareRegularChefRun) =
      new ChefRunPreparer(a, sshService, chefService) with RegularChefRun

  def chefRoleCreator(a: action.CreateChefRole) = new ChefRoleCreator(a, chefService)

  def chefDatabagCreator(a: action.CreateChefDatabag) = new ChefDatabagCreator(a, chefService)

  def chefEnvDestructor(a: action.DestroyChefEnv) = new ChefEnvDestructor(a, chefService)
}

object ChefPluginContextImpl {
    val CHEF_ENDPOINT = "chef.endpoint"
    val CacheRegion = "ChefCache"
}
