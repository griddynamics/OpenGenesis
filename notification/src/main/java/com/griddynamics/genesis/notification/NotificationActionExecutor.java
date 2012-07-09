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

import com.griddynamics.genesis.plugin.adapter.AbstractSyncActionExecutor;
import com.griddynamics.genesis.workflow.Action;
import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;

import javax.mail.Message;

public class NotificationActionExecutor extends AbstractSyncActionExecutor {

    private NotificationAction action;

    private EmailSenderConfiguration configuration;

    public NotificationActionExecutor(NotificationAction action, EmailSenderConfiguration configuration) {
        this.action = action;
        this.configuration = configuration;
    }

    @Override
    public NotificationResult startSync() {
        NotificationStep notificationStep = action.getNotificationStep();

        final Email email = new Email();
        email.setSubject(notificationStep.getSubject());
        email.setText(notificationStep.getMessage());

        for (String emailAddress : notificationStep.getEmails()) {
            email.addRecipient(null, emailAddress, Message.RecipientType.TO);
        }

        email.setFromAddress(configuration.getSenderName(), configuration.getSenderEmail());

        Mailer mailer = new Mailer(configuration.getSmtpHost(), configuration.getSmtpPort(), configuration.getSmtpUsername(),
                configuration.getSmtpPassword(), configuration.isUseTls() ? TransportStrategy.SMTP_TLS: TransportStrategy.SMTP_PLAIN);

        try {
            mailer.sendMail(email);
            return new NotificationResult(action, true);
        } catch (Exception e) {
            return new NotificationResult(action, false);
        }
    }

    public Action action() {
        return action;
    }

}
