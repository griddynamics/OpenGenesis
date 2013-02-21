package com.griddynamics.genesis.repository

/*
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

import org.squeryl.Schema
import org.springframework.beans.factory.InitializingBean
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.support.{TransactionTemplate, TransactionCallback}
import javax.sql.DataSource
import com.griddynamics.genesis.util.Logging

abstract class SchemaCreator[A <: Schema](val tablePattern: String) extends InitializingBean with Logging {
    def transactionManager: PlatformTransactionManager

    def schema: A

    def drop: Boolean

    def dataSource: DataSource

    val transactionTemplate: TransactionTemplate = new TransactionTemplate(transactionManager)

    def afterPropertiesSet() {
        if (drop)
            transactionTemplate.execute(new TransactionCallback[Unit]() {
                def doInTransaction(status: TransactionStatus) {
                    schema.drop
                }
            })

        if (drop || !isSchemaExists) {
            log.debug("Need to create schema %s", schema)
            transactionTemplate.execute(new TransactionCallback[Unit]() {
                def doInTransaction(status: TransactionStatus) {
                    schema.create
                }
            })
        }
    }

    def isSchemaExists = {
        var result: Boolean = false

        transactionTemplate.execute(new TransactionCallback[Unit]() {
            def doInTransaction(status: TransactionStatus) {
                val tables = DataSourceUtils.getConnection(dataSource).getMetaData
                  .getTables(null, null, tablePattern, Array("TABLE"))
                result = tables.next()
                tables.close()
                if (! result) { //workaround: db may be case-insensitive, but not return metadata about table names in lowercase
                    val tables = DataSourceUtils.getConnection(dataSource).getMetaData
                      .getTables(null, null, tablePattern.toUpperCase, Array("TABLE"))
                    result = tables.next()
                    tables.close()
                }
            }
        })

        result
    }
}
