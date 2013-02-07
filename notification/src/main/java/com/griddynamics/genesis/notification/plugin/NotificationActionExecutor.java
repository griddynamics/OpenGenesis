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

import com.griddynamics.genesis.plugin.adapter.AbstractSimpleSyncActionExecutor;
import com.griddynamics.genesis.workflow.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class NotificationActionExecutor extends AbstractSimpleSyncActionExecutor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private EmailSenderConfiguration configuration;


    public NotificationActionExecutor(NotificationAction action,
                                      EmailSenderConfiguration configuration) {
        super(action);
        this.configuration = configuration;
    }

    public JavaMailSender mailSender() {
        JavaMailSenderImpl result = new JavaMailSenderImpl();
        result.setHost(configuration.getSmtpHost());
        result.setPort(configuration.getSmtpPort());
        if (configuration.hasAuth()) {
            result.setUsername(configuration.getSmtpUsername());
            result.setPassword(configuration.getSmtpPassword());
        }
        Properties javamailProperties = new Properties();
        javamailProperties.setProperty("mail.smtp.timeout", configuration.getSmtpTimeout().toString());
        javamailProperties.setProperty("mail.smtp.connectiontimeout", configuration.getSmtpTimeout().toString());
        javamailProperties.setProperty("mail.smtps.timeout", configuration.getSmtpTimeout().toString());
        javamailProperties.setProperty("mail.smtps.connectiontimeout", configuration.getSmtpTimeout().toString());
        //javamailProperties.setProperty("mail.debug", "true");
        if (configuration.getUseTls()) {
            javamailProperties.setProperty("mail.smtp.starttls.enable", "true");
            javamailProperties.setProperty("mail.smtp.debug", "true");
        }
        if (configuration.getUseSSL()) {
            javamailProperties.setProperty("mail.smtps.starttls.enable", "true");
            javamailProperties.setProperty("mail.smtps.auth", "true");
            javamailProperties.setProperty("mail.smtps.debug", "true");
            result.setProtocol("smtps");
        }
        result.setJavaMailProperties(javamailProperties);
        return result;
    }

    @Override
    public ActionResult startSync() {
        final NotificationStep notificationStep = ((NotificationAction) getAction()).getNotificationStep();

        try {
            JavaMailSender sender = mailSender();
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            for (String s : notificationStep.getEmails()) {
                helper.addTo(s);
            }
            helper.setFrom(configuration.getSenderEmail(), configuration.getSenderName());
            helper.setSubject(notificationStep.getSubject());
            helper.setText(((NotificationAction) getAction()).getMessage());
            sender.send(mimeMessage);
            return new NotificationResult(getAction());
        } catch (MailSendException e) {
            log.error(e.getMessage(), e);
            return new NotificationResultFailed(getAction(), (e.getMessageExceptions() != null && e.getMessageExceptions().length > 0) ? e.getMessageExceptions()[0] : e);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return new NotificationResultFailed(getAction(), e.getCause() == null ? e : e.getCause());
        }
    }

}
