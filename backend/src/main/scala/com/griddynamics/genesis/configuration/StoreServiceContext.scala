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
package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Configuration, Bean}
import javax.sql.DataSource
import org.squeryl.{Session, SessionFactory}
import com.griddynamics.genesis.model.GenesisSchema
import org.squeryl.internals.DatabaseAdapter
import org.springframework.jdbc.datasource.{DataSourceUtils, DataSourceTransactionManager}
import com.griddynamics.genesis.repository.SchemaCreator
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.PlatformTransactionManager
import com.griddynamics.genesis.service.impl
import com.griddynamics.genesis.repository.impl.{ProjectPropertyRepository, ProjectRepository}
import impl.ProjectServiceImpl
import org.squeryl.adapters.{PostgreSqlAdapter, MSSQLServer, MySQLAdapter, H2Adapter}

@Configuration
class JdbcStoreServiceContext extends StoreServiceContext {

    @Bean def storeService = new impl.StoreService

    @Bean def projectRepository: com.griddynamics.genesis.repository.ProjectRepository = new ProjectRepository

    @Bean def projectService = new ProjectServiceImpl(projectRepository)

    @Bean def projectPropertyRepository = new ProjectPropertyRepository
}

class GenesisSchemaCreator(override val dataSource : DataSource, override val transactionManager : PlatformTransactionManager,
                           override val drop: Boolean) extends SchemaCreator[GenesisSchema](GenesisSchema.envs.name) {
    override val transactionTemplate = new TransactionTemplate(transactionManager)
    override val schema = GenesisSchema
}

object SquerylConfigurator {
    def createDatabaseAdapter(jdbcUrl : String) = {
        jdbcUrl.drop("jdbc:".length).takeWhile(_ != ':').toLowerCase match {
            case "h2" => new H2Adapter
            case "mysql" => new MySQLAdapter
            case "postgresql" => new PostgreSqlAdapter
            case "sqlserver" => new MSSQLServer
            case _ => throw new IllegalArgumentException
        }
    }
}

class SquerylTransactionManager(dataSource : DataSource,
                                defaultIsolationLevel : Int,
                                databaseAdapter : DatabaseAdapter) extends DataSourceTransactionManager {
    setDataSource(dataSource)

    override def afterPropertiesSet() {
        super.afterPropertiesSet()
        SessionFactory.externalTransactionManagementAdapter = Some(() => {
            if(Session.hasCurrentSession) {
                Session.currentSessionOption.get
            }
            else {
                val connection = DataSourceUtils.getConnection(getDataSource)
                connection.setTransactionIsolation(defaultIsolationLevel)

                val session = new Session(connection, databaseAdapter, None) {
                    override def cleanup = {
                        super.cleanup
                        unbindFromCurrentThread
                    }
                }
                session.bindToCurrentThread

                session
            }
        })
    }

    override def doCleanupAfterCompletion(transaction: AnyRef) {
        super.doCleanupAfterCompletion(transaction)
        Session.cleanupResources //clean up resources when done, following the doc
    }
}
