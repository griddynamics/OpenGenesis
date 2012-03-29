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

import com.griddynamics.genesis.service.StoreService
import org.springframework.context.annotation.{Configuration, Bean}
import org.apache.commons.dbcp.BasicDataSource
import com.griddynamics.genesis.service.impl
import org.springframework.beans.factory.annotation.Value
import javax.sql.DataSource
import org.squeryl.{Session, SessionFactory}
import com.griddynamics.genesis.model.GenesisSchema
import org.squeryl.internals.DatabaseAdapter
import org.springframework.jdbc.datasource.{DataSourceUtils, DataSourceTransactionManager}
import org.springframework.beans.factory.InitializingBean
import java.sql.Connection
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate}
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}
import org.squeryl.adapters.{MSSQLServer, MySQLAdapter, H2Adapter}

@Configuration
class JdbcStoreServiceContext extends StoreServiceContext {
    @Value("${genesis.jdbc.url}") var jdbcUrl : String = _
    @Value("${genesis.jdbc.username}") var jdbcUser : String = _
    @Value("${genesis.jdbc.password}") var jdbcPassword : String = _
    @Value("${genesis.jdbc.driver}") var jdbcDriver : String = _

    @Bean def storeService = new impl.StoreService

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
}

class GenesisSchemaCreator(dataSource : DataSource, transactionManager : PlatformTransactionManager) extends InitializingBean {
    @Value("${genesis.jdbc.drop.db:false}") var drop : Boolean = _

    val transactionTemplate = new TransactionTemplate(transactionManager)

    def afterPropertiesSet() {
        if(drop)
            transactionTemplate.execute(new TransactionCallback[Unit]() {
                def doInTransaction(status: TransactionStatus) {
                    GenesisSchema.drop
                }
            })

        if (drop || !isSchemaExists)
            transactionTemplate.execute(new TransactionCallback[Unit]() {
                def doInTransaction(status: TransactionStatus) {
                    GenesisSchema.create
                }
            })
    }

    def isSchemaExists() = {
        var result : Boolean = false

        transactionTemplate.execute(new TransactionCallback[Unit]() {
            def doInTransaction(status: TransactionStatus) {
                val tables = DataSourceUtils.getConnection(dataSource).getMetaData
                                            .getTables(null, null, "%", Array("TABLE"))
                result = tables.next()
                tables.close()
            }
        })

        result
    }
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
