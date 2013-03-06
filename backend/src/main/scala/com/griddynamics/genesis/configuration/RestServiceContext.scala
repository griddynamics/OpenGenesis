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

import com.griddynamics.genesis.frontend.GenesisRestService
import com.griddynamics.genesis.repository.ConfigurationRepository
import com.griddynamics.genesis.scheduler.EnvironmentJobService
import com.griddynamics.genesis.service.EnvironmentAccessService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class RestServiceContext {
    @Autowired var storeServiceContext : StoreServiceContext = _
    @Autowired var templateServiceContext : TemplateServiceContext = _
    @Autowired var computeServiceContext : ComputeServiceContext = _
    @Autowired var workflowContext : WorkflowContext = _
    @Autowired var envAccessService: EnvironmentAccessService = _
    @Autowired var configurationRepository: ConfigurationRepository = _
    @Autowired var envJobService: EnvironmentJobService = _

    @Bean def genesisRestService = new GenesisRestService(storeServiceContext.storeService,
                                                          templateServiceContext.templateService,
                                                          computeServiceContext.compService,
                                                          workflowContext.requestBroker,
                                                          envAccessService,
                                                          configurationRepository,
                                                          envJobService)
}
