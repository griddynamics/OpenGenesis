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
package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Configuration, Bean}
import com.griddynamics.genesis.steps.builder.ReflectionBasedStepBuilderFactory
import com.griddynamics.genesis.plugin.{StepDefinition, StepBuilderFactory}
import org.springframework.core.convert.support.DefaultConversionService
import com.griddynamics.genesis.service.impl.GroovyTemplateService
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.spring.ApplicationContextAware
import com.griddynamics.genesis.service.TemplateService
import javax.annotation.Resource
import com.griddynamics.genesis.template.{DependentListFactory, ListVarDSFactory, DataSourceFactory}
import com.griddynamics.genesis.template.support.{DatabagDataSourceFactory}
import com.griddynamics.genesis.cache.CacheManager

@Configuration
class GroovyTemplateServiceContext {
    @Autowired var templateRepositoryContext: TemplateRepositoryContext = _
    @Autowired var stepBuilderFactories: Array[StepBuilderFactory] = _
    @Autowired var genericStepDefinitions: Array[StepDefinition] = Array()
    @Autowired var varDataSourceFactories: Array[DataSourceFactory] = _
    @Autowired var storeServiceContext: StoreServiceContext = _
    @Autowired var cacheManager: CacheManager = _

    @Bean(name = Array("groovy")) def templateService = new GroovyTemplateService(
        templateRepositoryContext.templateRepository,
        stepBuilderFactories ++ genericStepDefinitions.map(definition => new ReflectionBasedStepBuilderFactory(definition.name, definition.step)),
        new DefaultConversionService,
        varDataSourceFactories, storeServiceContext.databagRepository,
        storeServiceContext.environmentService, cacheManager)
}

@Configuration
class DefaultTemplateServiceContext extends TemplateServiceContext with ApplicationContextAware {
    @Resource(name="${genesis.template.service:groovy}") var service: TemplateService = _
    @Bean def templateService = service
    @Bean def listVarDsFactory = new ListVarDSFactory
    @Bean def depList = new DependentListFactory
}

@Configuration
class CoreDataSourcesContext {
  @Autowired var storeServiceContext: StoreServiceContext = _

  @Bean def databagDsFactory = new DatabagDataSourceFactory(storeServiceContext.databagRepository)
}
