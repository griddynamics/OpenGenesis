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

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class EmailSenderConfiguration {

    private String senderName;

    private String senderEmail;

    private String smtpHost;

    private Integer smtpPort;

    private String smtpUsername;

    private String smtpPassword;

    private Boolean useTls;

    private Boolean useSSL;

    private Integer connectTimeout;
    private Integer smtpTimeout;

    public EmailSenderConfiguration(String senderName, String senderEmail, String smtpHost, Integer smtpPort,
                                    String smtpUsername, String smtpPassword, Boolean useTls,
                                    Integer connectTimeout, Integer smtpTimeout, Boolean useSSL) {
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.useTls = useTls;
        this.connectTimeout = connectTimeout;
        this.smtpTimeout = smtpTimeout;
        this.useSSL = useSSL;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public Boolean isUseTls() {
        return useTls;
    }

    public Boolean getUseTls() {
        return useTls;
    }

    public void setUseTls(Boolean useTls) {
        this.useTls = useTls;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSmtpTimeout() {
        return smtpTimeout;
    }

    public void setSmtpTimeout(Integer smtpTimeout) {
        this.smtpTimeout = smtpTimeout;
    }

    public Boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL = useSSL;
    }

    @Override
    public String toString() {
        return "EmailSenderConfiguration{" +
                "senderName='" + senderName + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", smtpUsername='" + smtpUsername + '\'' +
                ", smtpPassword='*************'" +
                ", useTls=" + useTls +
                ", useSSL=" + useSSL +
                ", connectTimeout=" + connectTimeout +
                ", smtpTimeout=" + smtpTimeout +
                '}';
    }
}
