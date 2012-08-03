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
package com.griddynamics.genesis.chef

import coordinator.ChefStepCoordinatorFactory
import executor._
import com.griddynamics.genesis.util.InputUtil
import com.griddynamics.genesis.configuration.{StoreServiceContext, ComputeServiceContext}
import com.griddynamics.genesis.exec.ExecPluginContext
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import com.griddynamics.genesis.service.{StoreService, SshService, Credentials}
import com.griddynamics.genesis.chef.rest.ChefRestClient
import step.ChefResourcesImpl
import com.griddynamics.genesis.plugin.PluginConfigurationContext
import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.cache.Cache
import net.sf.ehcache.CacheManager
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.crypto.BasicCrypto
import com.griddynamics.genesis.actions.json.PreprocessingJsonAction
import com.griddynamics.genesis.executors.json.PreprocessJsonActionExecutor

trait ChefPluginContext {
    def preprocessJsonAction(action: PreprocessingJsonAction): PreprocessJsonActionExecutor

    def chefNodeInitializer(a: action.InitChefNode): ChefNodeInitializer

    def initialChefRunPreparer(a: action.PrepareInitialChefRun): ChefRunPreparer[action.PrepareInitialChefRun]

    def regularChefRunPreparer(a: action.PrepareRegularChefRun): ChefRunPreparer[action.PrepareRegularChefRun]

    def chefDatabagCreator(a: action.CreateChefDatabag): ChefDatabagCreator

    def chefRoleCreator(a: action.CreateChefRole): ChefRoleCreator

    def chefEnvDestructor(a: action.DestroyChefEnv): ChefEnvDestructor
}

private object Plugin {
  val id = "chef"

  val ChefId = "genesis.plugin.chef.id"
  val Identity = "genesis.plugin.chef.identity"
  val Credential = "genesis.plugin.chef.credential"
  val ValidatorIdentity = "genesis.plugin.chef.validator.identity"
  val ValidatorCredential = "genesis.plugin.chef.validator.credential"
  val Endpoint = "genesis.plugin.chef.endpoint"
}

@Configuration
@GenesisPlugin(id="chef", description = "Chef plugin")
class ChefPluginContextImpl extends Cache {

    import ChefPluginContextImpl._

    @Autowired var computeServiceContext: ComputeServiceContext = _
    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var execPluginContext: ExecPluginContext = _

    @Autowired var pluginConfiguration: PluginConfigurationContext = _
    @Autowired var cacheManager: CacheManager = _

    @PostConstruct
    def initCache() {
      val cache = new net.sf.ehcache.Cache(
        CacheRegion, 100, false, false, TimeUnit.HOURS.toSeconds(2), TimeUnit.HOURS.toSeconds(1), false, 0);
      cacheManager.addCacheIfAbsent(cache)
    }

    def chefService(config: ChefPluginConfig) = new ChefServiceImpl(config.chefId , config.chefEndpoint,
        new Credentials(config.chefValidatorIdentity, config.chefValidatorCredential), chefClient(config))

    def chefClient(config: ChefPluginConfig) = {
      val key = ChefCacheKey(config.chefEndpoint, config.chefIdentity, config.chefCredentialResource)
      fromCache(ChefPluginContextImpl.CacheRegion, key) {
        new ChefRestClient(config.chefEndpoint, config.chefIdentity, BasicCrypto.privateKey(config.chefCredential))
      }
    }

  @Bean def chefStepCoordinatorFactory = {
    new ChefStepCoordinatorFactory(execPluginContext,
      () => {
        val configSnapshot = new ChefPluginConfig(pluginConfiguration.configuration(Plugin.id))

        new ChefExecutionContextImpl(
          computeServiceContext.sshService,
          chefService(configSnapshot),
          storeServiceContext.storeService,
          configSnapshot
        )
      }
    )
  }

  @Bean def chefRunStepBuilderFactory = new step.ChefRunStepBuilderFactory
  @Bean def createChefRoleBuilderFactory = new step.CreateChefRoleBuilderFactory
  @Bean def createChefDatabagBuilderFactory = new step.CreateChefDatabagBuilderFactory
  @Bean def destroyChefEnvStepBuilderFactory = new step.DestroyChefEnvStepBuilderFactory
}

private case class ChefCacheKey(endpoint: String, identity: String, credential: String)

class ChefPluginConfig(@transient config: Map[String, String]) extends Serializable {
  val chefId = config(Plugin.ChefId)

  val chefEndpoint = config(Plugin.Endpoint)

  val chefIdentity = config(Plugin.Identity)
  val chefCredentialResource = config(Plugin.Credential)

  val chefValidatorIdentity = config(Plugin.ValidatorIdentity)
  val chefValidatorCredentialResource = config(Plugin.ValidatorCredential)

  @transient lazy val chefCredential = InputUtil.locationAsString(chefCredentialResource)
  @transient lazy val chefValidatorCredential = InputUtil.locationAsString(chefValidatorCredentialResource)
  @transient lazy val chefResources = new ChefResourcesImpl("classpath:shell/chef-install.sh")
}



class ChefExecutionContextImpl(sshService: SshService,
                               chefService: ChefService,
                               storeService: StoreService,
                               config: ChefPluginConfig)
  extends ChefPluginContext {

  def chefNodeInitializer(a: action.InitChefNode) =
      new ChefNodeInitializer(a, sshService, chefService, storeService, config.chefResources)

  def initialChefRunPreparer(a: action.PrepareInitialChefRun) =
      new ChefRunPreparer(a, sshService, chefService) with InitialChefRun

  def regularChefRunPreparer(a: action.PrepareRegularChefRun) =
      new ChefRunPreparer(a, sshService, chefService) with RegularChefRun

  def chefRoleCreator(a: action.CreateChefRole) = new ChefRoleCreator(a, chefService)

  def chefDatabagCreator(a: action.CreateChefDatabag) = new ChefDatabagCreator(a, chefService)

  def chefEnvDestructor(a: action.DestroyChefEnv) = new ChefEnvDestructor(a, chefService)

  def preprocessJsonAction(action: PreprocessingJsonAction) = new PreprocessJsonActionExecutor(action, action.templatesUrl.getOrElse(""))
}

object ChefPluginContextImpl {
    val CHEF_ENDPOINT = "chef.endpoint"
    val CacheRegion = "chefContext"
}
