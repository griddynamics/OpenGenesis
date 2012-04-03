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
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.beans.factory.annotation.Value
import javax.sql.DataSource
import org.squeryl.{Session, SessionFactory}
import com.griddynamics.genesis.model.GenesisSchema
import org.squeryl.internals.DatabaseAdapter
import org.springframework.jdbc.datasource.{DataSourceUtils, DataSourceTransactionManager}
import java.sql.Connection
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.squeryl.adapters.{MSSQLServer, MySQLAdapter, H2Adapter}
import com.griddynamics.genesis.repository.impl.ProjectRepository
import com.griddynamics.genesis.repository.SchemaCreator
import com.griddynamics.genesis.service.impl
import org.springframework.core.io.ResourceLoader
import javax.annotation.Resource
import org.apache.commons.configuration._
import com.griddynamics.genesis.util.Closeables

@Configuration
class JdbcStoreServiceContext extends StoreServiceContext {
    @Value("${genesis.jdbc.url}") var jdbcUrl : String = _
    @Value("${genesis.jdbc.username}") var jdbcUser : String = _
    @Value("${genesis.jdbc.password}") var jdbcPassword : String = _
    @Value("${genesis.jdbc.driver}") var jdbcDriver : String = _
    @Value("#{systemProperties['backend.properties']}") var propResource: String = _
    @Resource var resourceLoader: ResourceLoader = _

    @Bean def storeService = new impl.StoreService

    @Bean def projectRepository = new ProjectRepository
  
    @Bean def dataSource = {
        val res = new BasicDataSource
        res.setDriverClassName(jdbcDriver)
        res.setUrl(jdbcUrl)
        res.setUsername(jdbcUser)
        res.setPassword(jdbcPassword)
        res
    }

    @Bean def squerylTransactionManager = new SquerylTransactionManager(dataSource,
        Connection.TRANSACTION_REPEATABLE_READ, SquerylConfigurator.createDatabaseAdapter(jdbcUrl))

    @Bean def genesisSchemaCreator = new GenesisSchemaCreator(dataSource, squerylTransactionManager)
    
    @Bean def config = {
        val propConfig = new PropertiesConfiguration
        val is = resourceLoader.getResource(propResource).getInputStream
        Closeables.using(is) {propConfig.load(_)}
        val dbConfig =
        new DatabaseConfiguration(dataSource, GenesisSchema.settings.name, "key", "value", true)
        import collection.JavaConversions.seqAsJavaList
        new CompositeConfiguration(Seq(dbConfig, propConfig))
    }
}

class GenesisSchemaCreator(override val dataSource : DataSource, override val transactionManager : PlatformTransactionManager) extends SchemaCreator[GenesisSchema] {
    @Value("${genesis.jdbc.drop.db:false}") var drop : Boolean = _
    override val transactionTemplate = new TransactionTemplate(transactionManager)
    override val schema = GenesisSchema
}

object SquerylConfigurator {
    def createDatabaseAdapter(jdbcUrl : String) = {
        jdbcUrl.drop("jdbc:".length).takeWhile(_ != ':').toLowerCase match {
            case "h2" => new H2Adapter
            case "mysql" => new MySQLAdapter
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
