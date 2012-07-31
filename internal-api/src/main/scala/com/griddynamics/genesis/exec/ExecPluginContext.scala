/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.exec

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.configuration.{StoreServiceContext, ComputeServiceContext}
import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.exec.action.{UploadScripts, RunExecWithArgs, RunExec, InitExecNode}

trait ExecPluginContext {
  def execNodeInitializer(action: InitExecNode): ExecNodeInitializer

  def execRunner(action: RunExec): ExecRunner

  def syncExecRunner(action: RunExecWithArgs): SyncExecRunner

  def execStepCoordinator(s: ExecRunStep, context: StepExecutionContext): ExecStepCoordinator

  def scriptsUploader(action: UploadScripts): ScriptUploader
}

@Configuration
class ExecPluginContextImpl extends ExecPluginContext {
  @Autowired var computeServiceContext: ComputeServiceContext = _
  @Autowired var storeServiceContext: StoreServiceContext = _

  @Bean def execResources = new ExecResourcesImpl

  @Bean def execStepCoordinatorFactory = new ExecStepCoordinatorFactory(this)

  @Bean def execStepBuilderFactory = new ExecRunStepBuilderFactory

  def execNodeInitializer(action: InitExecNode) =
    new ExecNodeInitializer(action, computeServiceContext.sshService,
      storeServiceContext.storeService, execResources)

  def execRunner(action: RunExec) =
    new ExecRunner(action, computeServiceContext.sshService)

  def syncExecRunner(action: RunExecWithArgs) =
    new SyncExecRunner(action, computeServiceContext.sshService)

  def execStepCoordinator(s: ExecRunStep, context: StepExecutionContext) =
    new ExecStepCoordinator(s, context, this, computeServiceContext.compService)

  def scriptsUploader(action: UploadScripts) =
    new ScriptUploader(action, computeServiceContext.sshService)
}
