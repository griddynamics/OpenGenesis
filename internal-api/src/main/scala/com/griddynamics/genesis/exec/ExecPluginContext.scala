package com.griddynamics.genesis.exec

/*
 * Copyright (c) 2011 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   For information about the licensing and copyright of this document please
 *   contact Grid Dynamics at info@griddynamics.com.
 *
 *   $Id: $
 *   @Project:     Genesis
 *   @Description: A cloud deployment platform
 */

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.configuration.{StoreServiceContext, ComputeServiceContext}
import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.exec.action.{RunExecWithArgs, RunExec, InitExecNode}

trait ExecPluginContext {
  def execNodeInitializer(action: InitExecNode): ExecNodeInitializer

  def execRunner(action: RunExec): ExecRunner

  def syncExecRunner(action: RunExecWithArgs): SyncExecRunner

  def execStepCoordinator(s: ExecRunStep, context: StepExecutionContext): ExecStepCoordinator
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
}
