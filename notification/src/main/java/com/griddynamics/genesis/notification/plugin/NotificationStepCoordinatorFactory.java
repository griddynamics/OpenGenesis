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
package com.griddynamics.genesis.notification.plugin;

import com.griddynamics.genesis.notification.service.MailServiceProvider;
import com.griddynamics.genesis.plugin.PluginConfigurationContext;
import com.griddynamics.genesis.plugin.StepExecutionContext;
import com.griddynamics.genesis.plugin.adapter.AbstractPartialStepCoordinatorFactory;
import com.griddynamics.genesis.workflow.Step;
import com.griddynamics.genesis.workflow.StepCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationStepCoordinatorFactory extends AbstractPartialStepCoordinatorFactory {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MailServiceProvider provider;

    public NotificationStepCoordinatorFactory(String pluginId, PluginConfigurationContext pluginConfiguration, MailServiceProvider provider) {
        super(pluginId, pluginConfiguration);
        this.provider = provider;
    }

    @Override
    public boolean isDefinedAt(Step step) {
        return step instanceof NotificationStep;
    }

    @Override
    public StepCoordinator apply(Step step, StepExecutionContext context) {
        return new NotificationStepCoordinator(context, (NotificationStep) step, provider.emailSenderConfiguration(), provider.getTemplateEngine());
    }
}
