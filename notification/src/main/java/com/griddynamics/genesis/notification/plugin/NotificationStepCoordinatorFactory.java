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

import com.griddynamics.genesis.notification.template.StringTemplateEngine;
import com.griddynamics.genesis.notification.template.TemplateEngine;
import com.griddynamics.genesis.plugin.PluginConfigurationContext;
import com.griddynamics.genesis.plugin.StepExecutionContext;
import com.griddynamics.genesis.plugin.adapter.AbstractPartialStepCoordinatorFactory;
import com.griddynamics.genesis.workflow.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import scala.collection.JavaConversions;

import java.util.Properties;

public class NotificationStepCoordinatorFactory extends AbstractPartialStepCoordinatorFactory {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public NotificationStepCoordinatorFactory(String pluginId, PluginConfigurationContext pluginConfiguration) {
        super(pluginId, pluginConfiguration);
    }

    @Override
    public boolean isDefinedAt(Step step) {
        return step instanceof NotificationStep;
    }

    @Override
    public StepCoordinator apply(Step step, StepExecutionContext context) {
        return new NotificationStepCoordinator(context, (NotificationStep) step, emailSenderConfiguration(), getTemplateEngine());
    }

    public EmailSenderConfiguration emailSenderConfiguration() {
        java.util.Map<String,String> config = getConfig();
        String senderName = config.get(NotificationPluginConfig.senderName);
        String senderEmail = config.get(NotificationPluginConfig.senderEmail);
        String smtpHost = config.get(NotificationPluginConfig.smtpHost);
        Integer smtpPort = getIntParameter(config, NotificationPluginConfig.smtpPort);
        String smtpUsername = config.get(NotificationPluginConfig.smtpUsername);
        String smtpPassword = config.get(NotificationPluginConfig.smtpPassword);
        Boolean useTls = Boolean.parseBoolean(config.get(NotificationPluginConfig.useTls));
        Boolean useSSL = Boolean.parseBoolean(config.get(NotificationPluginConfig.useSSL));
        Integer connectTimeout = getIntParameter(config, NotificationPluginConfig.connectTimeout);
        Integer smtpTimeout = getIntParameter(config, NotificationPluginConfig.smtpTimeout);
        return new EmailSenderConfiguration(senderName, senderEmail, smtpHost, smtpPort,
                smtpUsername, smtpPassword, useTls, connectTimeout, smtpTimeout, useSSL);
    }

    private static Integer getIntParameter(java.util.Map<String, String> config, String paramName) {
        Integer result;
        String paramValue = config.get(paramName);
        if (null != paramValue && StringUtils.isNumeric(paramValue)) {
            try {
                return Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(paramName + " is out of scope of int: " + "%s");
            }
        } else {
            throw new IllegalArgumentException(paramName + "must be a number, but it set to " + String.valueOf(paramValue));
        }
    }

    private TemplateEngine getTemplateEngine() {
        String templateFolder = getConfig().get(NotificationPluginConfig.templateFolder);
        TemplateEngine templateEngine;
        try {
            templateEngine = new StringTemplateEngine(templateFolder);
        } catch (IllegalArgumentException e) {
            log.error("Error creating templates group: " + e.getMessage(), e);
            templateEngine = new StringTemplateEngine(System.getProperty("user.dir"));
        }
        return templateEngine;
    }

}
