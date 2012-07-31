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

import com.griddynamics.genesis.plugin.adapter.AbstractStep;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public class NotificationStep extends AbstractStep {

  private List<String> emails;
  private String subject;
  private String templateName;
  private Map<String, String> templateParams;

  public NotificationStep(List<String> emails, String subject, String templateName, Map<String, String> templateParams) {
    this.emails = emails;
    this.subject = subject;
    this.templateName = templateName;
    this.templateParams = templateParams;
  }

  public List<String> getEmails() {
    return emails;
  }

  public String getTemplateName() {
    return templateName;
  }

  public String getSubject() {
    return subject;
  }

  public Map<String, String> getTemplateParams() {
    return templateParams;
  }

  public String stepDescription() {
    return String.format("Sends email notification to [%s]; subject: %s", StringUtils.join(emails, ","), subject);
  }

  @Override
  public String toString() {
    return "NotificationStep{" +
        "emails=" + emails +
        ", subject='" + subject + '\'' +
        '}';
  }
}
