/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.notification.service;

import com.griddynamics.genesis.configuration.MailServiceContext;
import com.griddynamics.genesis.notification.plugin.EmailSenderConfiguration;
import com.griddynamics.genesis.notification.plugin.NotificationPluginConfig;
import com.griddynamics.genesis.notification.template.StringTemplateEngine;
import com.griddynamics.genesis.notification.template.TemplateEngine;
import com.griddynamics.genesis.plugin.PluginConfigurationContext;
import com.griddynamics.genesis.service.EmailService;
import scala.collection.JavaConversions;

import java.util.Map;

import static com.griddynamics.genesis.notification.utils.ConfigReaderUtils.getIntParameter;
import static com.griddynamics.genesis.notification.utils.ConfigReaderUtils.getRequiredParameter;

public class MailServiceProvider implements MailServiceContext {

    private final String pluginId;
    private final PluginConfigurationContext pluginConfiguration;

    public MailServiceProvider(String pluginId, PluginConfigurationContext pluginConfiguration) {
        this.pluginId = pluginId;
        this.pluginConfiguration = pluginConfiguration;
    }

    public Map<String, String> getConfig() {
        return JavaConversions.mapAsJavaMap(pluginConfiguration.configuration(pluginId));
    }

    public EmailSenderConfiguration emailSenderConfiguration() {
        java.util.Map<String,String> config = getConfig();
        String senderName = config.get(NotificationPluginConfig.senderName);
        String senderEmail = getRequiredParameter(config, NotificationPluginConfig.senderEmail);
        String smtpHost = getRequiredParameter(config, NotificationPluginConfig.smtpHost);
        Integer smtpPort = getIntParameter(config, NotificationPluginConfig.smtpPort, Short.MAX_VALUE * 2);
        String smtpUsername = config.get(NotificationPluginConfig.smtpUsername);
        String smtpPassword = config.get(NotificationPluginConfig.smtpPassword);
        Boolean useTls = Boolean.parseBoolean(config.get(NotificationPluginConfig.useTls));
        Boolean useSSL = Boolean.parseBoolean(config.get(NotificationPluginConfig.useSSL));
        Integer connectTimeout = getIntParameter(config, NotificationPluginConfig.connectTimeout, Integer.MAX_VALUE);
        Integer smtpTimeout = getIntParameter(config, NotificationPluginConfig.smtpTimeout, Integer.MAX_VALUE);
        return new EmailSenderConfiguration(senderName, senderEmail, smtpHost, smtpPort,
                smtpUsername, smtpPassword, useTls, connectTimeout, smtpTimeout, useSSL);
    }

    public EmailService getEmailService() {
        return new SpringBasedEmailService(emailSenderConfiguration(), getTemplateEngine());
    }

    public TemplateEngine getTemplateEngine() {
        String templateFolder = getConfig().get(NotificationPluginConfig.templateFolder);
        TemplateEngine templateEngine;
        templateEngine = new StringTemplateEngine(templateFolder);
        return templateEngine;
    }
}
