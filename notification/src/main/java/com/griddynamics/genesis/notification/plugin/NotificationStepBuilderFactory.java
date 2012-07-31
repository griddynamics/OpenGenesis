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

import com.griddynamics.genesis.plugin.StepBuilder;
import com.griddynamics.genesis.plugin.StepBuilderFactory;
import com.griddynamics.genesis.plugin.adapter.AbstractStepBuilder;
import com.griddynamics.genesis.workflow.Step;

import java.util.List;
import java.util.Map;

public class NotificationStepBuilderFactory implements StepBuilderFactory {

  @Override
  public final String stepName() {
    return "notify";
  }

  @Override
  public StepBuilder newStepBuilder() {
    return new AbstractStepBuilder() {

      private List<String> emails;
      private String subject;
      private String templateName;
      private Map<String, String> templateParams;

      public List<String> getEmails() {
        return emails;
      }

      public void setEmails(List<String> emails) {
        this.emails = emails;
      }

      public String getTemplateName() {
        return templateName;
      }

      public void setTemplateName(String templateName) {
        this.templateName = templateName;
      }

      public String getSubject() {
        return subject;
      }

      public void setSubject(String subject) {
        this.subject = subject;
      }

      public Map<String, String> getTemplateParams() {
        return templateParams;
      }

      public void setTemplateParams(Map<String, String> templateParams) {
        this.templateParams = templateParams;
      }

      @Override
      public Step getDetails() {
        return new NotificationStep(emails, subject, templateName, templateParams);
      }
    };
  }


}
