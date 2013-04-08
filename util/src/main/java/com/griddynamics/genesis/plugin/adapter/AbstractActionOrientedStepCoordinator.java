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
package com.griddynamics.genesis.plugin.adapter;

import com.griddynamics.genesis.model.VariablesField;
import com.griddynamics.genesis.plugin.GenesisStepResult;
import com.griddynamics.genesis.plugin.StepExecutionContext;
import com.griddynamics.genesis.workflow.ActionOrientedStepCoordinator;
import com.griddynamics.genesis.workflow.Step;
import com.griddynamics.genesis.workflow.StepResult;
import scala.Option;
import scala.collection.JavaConversions;

import java.util.Map;

public abstract class AbstractActionOrientedStepCoordinator implements ActionOrientedStepCoordinator {

  private StepExecutionContext context;

  private AbstractStep step;

  private Option<StepResult> result;

  protected AbstractActionOrientedStepCoordinator(StepExecutionContext context, AbstractStep step) {
    this.context = context;
    this.step = step;
  }

  @Override
  public StepResult getStepResult() {
    return new GenesisStepResult(context.step(), isFailed(), context.envUpdate(), context.serversUpdate(), result);
  }

  protected abstract boolean isFailed();

  @Override
  public Step step() {
    return step;
  }

  public void setResult(Option<StepResult> result) {
    this.result = result;
  }

  protected Map<String, String> getContextVariables() {
    return JavaConversions.mapAsJavaMap(VariablesField.variablesFieldToMap(context.workflow().variables()));
  }
}