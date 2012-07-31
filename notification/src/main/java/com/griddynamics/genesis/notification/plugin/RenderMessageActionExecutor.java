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

import com.griddynamics.genesis.notification.template.TemplateEngine;
import com.griddynamics.genesis.plugin.adapter.AbstractSimpleSyncActionExecutor;
import com.griddynamics.genesis.workflow.Action;
import com.griddynamics.genesis.workflow.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RenderMessageActionExecutor extends AbstractSimpleSyncActionExecutor {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private TemplateEngine templateEngine;
  private Map<String, String> params;

  public RenderMessageActionExecutor(RenderMessageAction action,
                                     TemplateEngine templateEngine,
                                     Map<String, String> contextVariables) {
    super(action);
    this.templateEngine = templateEngine;
    if (action.getNotificationStep().getTemplateParams() != null) {
      params = new HashMap<String, String>();
      params.putAll(contextVariables);
      params.putAll(action.getNotificationStep().getTemplateParams());
    } else {
      params = contextVariables;
    }
  }

  @Override
  public ActionResult startSync() {
    NotificationStep notificationStep = ((RenderMessageAction) getAction()).getNotificationStep();

    try {

      String message = templateEngine.renderText(notificationStep.getTemplateName(), params);
      return new RenderMessageResult(getAction(), message);

    } catch (IllegalArgumentException e) {

      log.error(e.getMessage(), e);
      return new RenderMessageResultFailed(getAction());

    }
  }

}
