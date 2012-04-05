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
import javax.annotation.Resource
import org.springframework.core.io.ResourceLoader
import com.griddynamics.genesis.util.Closeables
import org.springframework.beans.factory.annotation._
import com.griddynamics.genesis.service.impl
import reflect.BeanProperty
import java.util.Properties
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.apache.commons.configuration.{ConfigurationConverter, CompositeConfiguration, PropertiesConfiguration, Configuration => CommonsConfig}

@Configuration
class DefaultConfigServiceContext extends ConfigServiceContext {
    @Autowired private var dbConfig : CommonsConfig = _
    @Autowired private var fileProps: PropertiesFactoryBean = _

    private lazy val config = {
        val compConfig = new CompositeConfiguration
        compConfig.addConfiguration(dbConfig, true) // updates go to DB, reads are from DB first
        compConfig.addConfiguration(ConfigurationConverter.getConfiguration(fileProps.getObject)) // file properties are read after DB
        compConfig
    }

    @Bean def configService = new impl.DefaultConfigService(config)

    @BeanProperty var dbProps:Properties = _

}
