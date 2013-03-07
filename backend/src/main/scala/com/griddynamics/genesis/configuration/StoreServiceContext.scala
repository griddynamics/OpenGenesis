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

import org.springframework.context.annotation.{Configuration, Bean}
import javax.sql.DataSource
import org.squeryl.{Session, SessionFactory}
import com.griddynamics.genesis.model.{GenesisVersion, GenesisSchema}
import org.squeryl.internals.DatabaseAdapter
import org.springframework.jdbc.datasource.{DataSourceUtils, DataSourceTransactionManager}
import org.squeryl.adapters.{PostgreSqlAdapter, MySQLAdapter, H2Adapter}
import com.griddynamics.genesis.repository
import com.griddynamics.genesis.service
import repository.{DatabagTemplateRepository, GenesisVersionRepository, SchemaCreator}
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.PlatformTransactionManager
import com.griddynamics.genesis.adapters.MSSQLServerWithPagination
import com.griddynamics.genesis.service.{ProjectService, EnvironmentConfigurationService, impl}
import com.griddynamics.genesis.service.impl._
import org.springframework.beans.factory.InitializingBean
import com.griddynamics.genesis.util.Logging
import org.springframework.beans.factory.annotation.Autowired
import java.sql.SQLException
import com.griddynamics.genesis.validation.ConfigValueValidator
import collection.JavaConversions._
import scala.Some

@Configuration
class JdbcStoreServiceContext extends StoreServiceContext {

    @Autowired var projectAuthority: service.ProjectAuthorityService = _

    @Autowired var permissionService: PermissionService = _

    @Autowired var databagTemplateRepository: DatabagTemplateRepository = _

    @Autowired(required = false) private var validators: java.util.Map[String, ConfigValueValidator] = mapAsJavaMap(Map())

    @Bean def environmentSecurity: service.EnvironmentAccessService = new impl.EnvironmentAccessService(storeService, permissionService)

    @Bean def storeService: service.StoreService = new impl.StoreService

    @Bean def projectRepository: repository.ProjectRepository = new repository.impl.ProjectRepository

    @Bean def projectService: ProjectService = new ProjectServiceImpl(projectRepository, storeService, projectAuthority, configurationRepository)

    @Bean def credentialsRepository: repository.CredentialsRepository = new repository.impl.CredentialsRepository
    @Bean def configurationRepository: repository.ConfigurationRepository = new repository.impl.ConfigurationRepositoryImpl
    @Bean def environmentService: EnvironmentConfigurationService = new EnvironmentConfigurationServiceImpl(configurationRepository, environmentSecurity, databagTemplateRepository, validators.toMap)
    @Bean def credentialsStoreService: service.CredentialsStoreService = new impl.CredentialsStoreService(credentialsRepository, projectRepository)
    @Bean def serversArrayRepository: repository.ServerArrayRepository = new repository.impl.ServerArrayRepository()
    @Bean def serversRepository: repository.ServerRepository = new repository.impl.ServerRepository()
    @Bean def serversService: service.ServersService = new ServersServiceImpl(serversArrayRepository, serversRepository)
    @Bean def serversLoanService: service.ServersLoanService = new ServersLoanServiceImpl(storeService, credentialsRepository)
    @Bean def databagRepository: repository.DatabagRepository = new repository.impl.DatabagRepository
    @Bean def databagService: service.DataBagService = new impl.DataBagServiceImpl(databagRepository, databagTemplateRepository, validators.toMap)

}

class GenesisSchemaCreator(override val dataSource : DataSource, override val transactionManager : PlatformTransactionManager,
                           override val drop: Boolean, val buildInfoProps: java.util.Properties, val repo: GenesisVersionRepository) extends SchemaCreator[GenesisSchema](GenesisSchema.envs.name) {
    override val transactionTemplate = new TransactionTemplate(transactionManager)
    override val schema = GenesisSchema

    override def afterPropertiesSet() {
        val setNeeded = drop || !isSchemaExists
        super.afterPropertiesSet
        if (setNeeded) GenesisVersion.fromBuildProps(buildInfoProps).foreach(repo.set(_))
    }
}

class GenesisSchemaValidator(val repo: GenesisVersionRepository, val buildInfoProps: java.util.Properties)
  extends InitializingBean with Logging {

  def afterPropertiesSet() {
    validate()
  }

  def validate() {
    val schemaVersion = try {
      Some(repo.get)
    } catch {
      case e: Exception => log.error(e, "Couldn't retrieve schema version"); None
    }
    val genesisVersion = GenesisVersion.fromBuildProps(buildInfoProps)
    if (genesisVersion.isEmpty)
        throw new RuntimeException("Invalid application or DB schema version: No genesis version found at build info properties")
    if (schemaVersion.isEmpty)
        throw new RuntimeException("Invalid application or DB schema version: No genesis version found in database")
    if (genesisVersion != schemaVersion)
        throw new RuntimeException("Application and schema versions mismatch. Application version: %s, schema version: %s".format(genesisVersion.map(_.versionId).get, schemaVersion.get.versionId))
  }
}

object SquerylConfigurator {
    def createDatabaseAdapter(jdbcUrl : String) = {
        jdbcUrl.drop("jdbc:".length).takeWhile(_ != ':').toLowerCase match {
            case "h2" => new H2Adapter
            case "mysql" => new MySQLAdapter
            case "postgresql" => new PostgreSqlAdapter
            case "sqlserver" => new MSSQLServerWithPagination
            case _ => throw new IllegalArgumentException
        }
    }
}

class SquerylTransactionManager(dataSource : DataSource,
                                defaultIsolationLevel : Int,
                                databaseAdapter : DatabaseAdapter,
                                logSql: Boolean) extends DataSourceTransactionManager with Logging {
    setDataSource(dataSource)

    override def afterPropertiesSet() {
      super.afterPropertiesSet()
      val transactionManagementAdapter = Some(() => {
        SquerylTransactionManager.currentSession.get orElse {
          log.debug("Requesting a new session")
          val connection = DataSourceUtils.getConnection(getDataSource)
          connection.setTransactionIsolation(defaultIsolationLevel)
          val session = new Session(connection, databaseAdapter, None) with Logging {

            override def cleanup {
              super.cleanup
              unbindFromCurrentThread
              SquerylTransactionManager.currentSession.remove()
              //unfortunately without it connections are hanging
              try {
                this.connection.close()
              } catch {
                case e: SQLException => log.error("Error closing database connection", e)
              }
            }

          }
          if (logSql) session.setLogger(msg => println(msg))
          session.bindToCurrentThread
          SquerylTransactionManager.currentSession.set(Some(session))
          Some(session)
        }
      })
      SessionFactory.externalTransactionManagementAdapter = transactionManagementAdapter
    }

    override def doCleanupAfterCompletion(transaction: AnyRef) {
        super.doCleanupAfterCompletion(transaction)
        Session.cleanupResources //clean up resources when done, following the doc
    }
}

object SquerylTransactionManager {
  val currentSession: ThreadLocal[Option[Session]] = new ThreadLocal[Option[Session]]() {
    override def initialValue() = None
  }
}
