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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */

package com.griddynamics.genesis.configuration

import org.springframework.context.annotation._
import org.springframework.beans.factory.annotation._
import com.griddynamics.genesis.service.impl
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.transaction.support.{TransactionCallback, TransactionTemplate}
import org.springframework.transaction.{TransactionStatus, PlatformTransactionManager}
import org.apache.commons.configuration.{ConfigurationUtils, ConfigurationConverter, AbstractConfiguration}

@Configuration
class DefaultConfigServiceContext extends ConfigServiceContext {
    @Autowired private var dbConfig : AbstractConfiguration = _
    @Autowired private var fileProps: PropertiesFactoryBean = _
    @Autowired private var transactionManager : PlatformTransactionManager = _

    private lazy val transactionTemplate: TransactionTemplate = new TransactionTemplate(transactionManager)

    private lazy val config = {
        ConfigurationUtils.enableRuntimeExceptions(dbConfig)
        // read properties from file
        val mapConfig = ConfigurationConverter.getConfiguration(fileProps.getObject)
        // override them by DB properties
        ConfigurationUtils.copy(dbConfig, mapConfig)
        // write the result into DB:
        transactionTemplate.execute(new TransactionCallback[Unit] {
            def doInTransaction(p1: TransactionStatus) = dbConfig.copy(mapConfig)
        })
        dbConfig
    }

    @Bean def configService = new impl.DefaultConfigService(config)

}
