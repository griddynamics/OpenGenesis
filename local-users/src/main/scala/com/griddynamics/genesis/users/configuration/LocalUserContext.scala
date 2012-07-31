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

package com.griddynamics.genesis.users.configuration

import com.griddynamics.genesis.users.UserServiceContext
import org.springframework.context.annotation.{Bean, Configuration}
import javax.sql.DataSource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.beans.factory.annotation.{Value, Autowired}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.repository.SchemaCreator
import com.griddynamics.genesis.users.service.{LocalGroupService, LocalUserService}
import com.griddynamics.genesis.users.repository.{LocalUserGroupManagement, LocalGroupRepository, LocalUserRepository, LocalUserSchema}
import com.griddynamics.genesis.groups.GroupService

@Configuration
class LocalUserContext extends UserServiceContext with Logging {
    @Autowired var dataSource: DataSource = _
    @Autowired var transactionManager: PlatformTransactionManager = _

    @Value("#{fileProps['genesis.system.jdbc.drop.db'] ?: false}") var dropSchema: Boolean = _
    lazy val groupRepo = new LocalGroupRepositoryImpl
    @Bean def userService = new LocalUserService(new LocalUserRepository, groupService)
    @Bean def groupService : GroupService = new LocalGroupService(groupRepo)
    @Bean def schemaCreator = {
        new UsersSchemaCreator(dropSchema, dataSource, transactionManager)
    }
}
class LocalGroupRepositoryImpl extends LocalGroupRepository with LocalUserGroupManagement
class UsersSchemaCreator(val drop: Boolean, override val dataSource: DataSource, override val transactionManager: PlatformTransactionManager)
  extends SchemaCreator[LocalUserSchema](LocalUserSchema.users.name) {
    override val schema = LocalUserSchema
}

