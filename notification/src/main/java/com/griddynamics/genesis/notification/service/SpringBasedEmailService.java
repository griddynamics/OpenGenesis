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

import com.griddynamics.genesis.annotation.RemoteGateway;
import com.griddynamics.genesis.notification.plugin.EmailSenderConfiguration;
import com.griddynamics.genesis.notification.template.TemplateEngine;
import com.griddynamics.genesis.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@RemoteGateway("Email notification sender")
public class SpringBasedEmailService implements EmailService {

    private final EmailSenderConfiguration configuration;
    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    public SpringBasedEmailService(EmailSenderConfiguration configuration, TemplateEngine templateEngine) {
        this.configuration = configuration;
        this.templateEngine = templateEngine;
        this.mailSender = mailSender();
    }

    private JavaMailSender mailSender() {
        JavaMailSenderImpl result = new JavaMailSenderImpl();
        result.setHost(configuration.getSmtpHost());
        result.setPort(configuration.getSmtpPort());
        if (configuration.getSmtpUsername() != null) {
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
    public void sendEmail(List<String> recipients, String subject, String message) {
        if(recipients.isEmpty()){
            throw new IllegalArgumentException("Recipients list should not be empty");
        }
        try {
            JavaMailSender sender = this.mailSender;
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            for (String s : recipients) {
                helper.addTo(s);
            }
            helper.setFrom(configuration.getSenderEmail(), configuration.getSenderName());
            helper.setSubject(subject);
            helper.setText(message);
            sender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to create message", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to create message", e);
        }
    }

    @Override
    public void sendEmail(List<String> recipients, String subject, String templateName, Map<String, String> templateParameters) {
        String message = templateEngine.renderText(templateName, templateParameters);
        this.sendEmail(recipients, subject, message);
    }
}
