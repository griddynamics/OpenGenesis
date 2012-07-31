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
import com.griddynamics.genesis.plugin.StepExecutionContext;
import com.griddynamics.genesis.plugin.adapter.AbstractActionOrientedStepCoordinator;
import com.griddynamics.genesis.plugin.utils.ScalaUtils;
import com.griddynamics.genesis.workflow.*;
import scala.collection.Seq;

@SuppressWarnings("unchecked")
public class NotificationStepCoordinator extends AbstractActionOrientedStepCoordinator implements StepCoordinator {

  private EmailSenderConfiguration emailSenderConfiguration;
  private TemplateEngine templateEngine;

  private boolean failed = false;

  public NotificationStepCoordinator(StepExecutionContext context,
                                     NotificationStep step,
                                     EmailSenderConfiguration emailSenderConfiguration,
                                     TemplateEngine templateEngine) {
    super(context, step);
    this.emailSenderConfiguration = emailSenderConfiguration;
    this.templateEngine = templateEngine;
  }

  @Override
  public Seq onStepStart() {
    return ScalaUtils.list(new RenderMessageActionExecutor(new RenderMessageAction((NotificationStep) step()), templateEngine, getContextVariables()));
  }

  @Override
  public Seq onActionFinish(ActionResult result) {
    if (result instanceof RenderMessageResult) {
      String message = ((RenderMessageResult) result).getMessage();
      return ScalaUtils.list(new NotificationActionExecutor(new NotificationAction((NotificationStep) step(), message), emailSenderConfiguration));
    } else if (result instanceof NotificationResult) {
      failed = false;
    } else if (result instanceof ActionFailed) {
      failed = true;
    }
    return ScalaUtils.list();
  }

  @Override
  public Seq onStepInterrupt(Signal signal) {
    return ScalaUtils.list();
  }

  @Override
  public ActionExecutor getActionExecutor(Action action) {
    if (action instanceof RenderMessageAction) {
      return new RenderMessageActionExecutor((RenderMessageAction) action, templateEngine, getContextVariables());
    } else if (action instanceof NotificationAction) {
      return new NotificationActionExecutor((NotificationAction) action, emailSenderConfiguration);
    } else {
      throw new RuntimeException("Invalid action type");
    }
  }

  @Override
  protected boolean isFailed() {
    return failed;
  }

}
