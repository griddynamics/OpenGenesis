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
 *   @Description: E-mail notifications plugin
 */
package com.griddynamics.genesis.notification;

import com.griddynamics.genesis.plugin.api.GenesisPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@GenesisPlugin(id = "notification", description = "Sends basic notification to given emails")
public class NotificationPluginContext {

    @Value("${genesis.plugin.notification.sender.name}")
    private String senderName;

    @Value("${genesis.plugin.notification.sender.email}")
    private String senderEmail;

    @Value("${genesis.plugin.notification.smtp.host}")
    private String smtpHost;

    @Value("${genesis.plugin.notification.smtp.port}")
    private Integer smtpPort;

    @Value("${genesis.plugin.notification.smtp.username}")
    private String smtpUsername;

    @Value("${genesis.plugin.notification.smtp.password}")
    private String smtpPassword;

    @Value("${genesis.plugin.notification.smtp.useTls}")
    private boolean useTls;

    @Bean
    public NotificationStepBuilderFactory notificationStepBuilderFactory() {
        return new NotificationStepBuilderFactory();
    }

    @Bean
    public NotificationStepCoordinatorFactory notificationStepCoordinatorFactory() {
        return new NotificationStepCoordinatorFactory(getEmailSenderConfiguration());
    }

    private EmailSenderConfiguration getEmailSenderConfiguration() {
        return new EmailSenderConfiguration(senderName, senderEmail, smtpHost, smtpPort,
                smtpUsername, smtpPassword, useTls);
    }

}
