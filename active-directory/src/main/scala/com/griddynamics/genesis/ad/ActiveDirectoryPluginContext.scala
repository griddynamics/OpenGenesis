/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.ad

import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.plugin.api.GenesisPlugin
import com4j.typelibs.activeDirectory.IADs
import com4j.COM4J
import java.lang.String
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService, ConfigService}
import com.griddynamics.genesis.service.GenesisSystemProperties._
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.spring.security.AuthProviderFactory
import service.{ActiveDirectoryGroupServiceImpl, ActiveDirectoryUserServiceImpl}

@Configuration
@GenesisPlugin(id = "active-directory", description = "Genesis Active Directory Plugin")
class ActiveDirectoryPluginContext extends Logging {

  @Autowired var configService : ConfigService = _

  @Autowired var authorityService: AuthorityService = _

  @Autowired var projectAuthorityService: ProjectAuthorityService = _

  private[this] lazy val defaultNamingContext = {
    val rootDSE: IADs = COM4J.getObject(classOf[IADs], "LDAP://RootDSE", null)

    val context = rootDSE.get("defaultNamingContext").asInstanceOf[String]

    log.info("Windows Default Naming Context: %s", context)

    context
  }

  @Bean val activeDirectoryConnectionPool = new ActiveDirectorySingleConnectionPool

  private val template = new CommandTemplate(activeDirectoryConnectionPool)

  @Bean def activeDirectoryUserService =
    new ActiveDirectoryUserServiceImpl(defaultNamingContext, new ActiveDirectoryPluginConfig(configService), template)

  @Bean def activeDirectoryGroupService =
    new ActiveDirectoryGroupServiceImpl(defaultNamingContext, new ActiveDirectoryPluginConfig(configService), template)

  @Bean def adAuthProviderFactory = new AuthProviderFactory {
    val mode = "ad"

    def create() = new ActiveDirectoryAuthenticationProvider(defaultNamingContext,
      new ActiveDirectoryPluginConfig(configService), projectAuthorityService, authorityService, template)
  }

}

object ActiveDirectoryPluginContext {
  val PREFIX_AD = PLUGIN_PREFIX + ".active-directory."
  val USE_DOMAIN = PREFIX_AD + "use.domain"
}

class ActiveDirectoryPluginConfig(val configService: ConfigService) {
  import ActiveDirectoryPluginContext._

  def useDomain: Boolean = configService.get(USE_DOMAIN, false)
}
