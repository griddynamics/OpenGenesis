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
package com.griddynamics.genesis.chef.coordinator

import com.griddynamics.genesis.plugin.{StepExecutionContext, PartialStepCoordinatorFactory}
import com.griddynamics.genesis.workflow.{ActionStepCoordinator, Step}
import com.griddynamics.genesis.chef.step._
import com.griddynamics.genesis.exec.ExecPluginContext
import com.griddynamics.genesis.chef.{action, ChefPluginContext}

class ChefStepCoordinatorFactory(execPluginContext: ExecPluginContext,
                                 chefPluginContextProvider: () => ChefPluginContext)
  extends PartialStepCoordinatorFactory {

  def isDefinedAt(step: Step) = step.isInstanceOf[ChefStep]

  def apply(step: Step, context: StepExecutionContext) = {

    val chefPluginContext = context.pluginContexts.getOrElseUpdate("chef", chefPluginContextProvider()).asInstanceOf[ChefPluginContext];

    step match {
      case s: ChefRun => new ChefRunCoordinator(s, context, execPluginContext, chefPluginContext)

      case s: CreateChefDatabag => new ActionStepCoordinator(
        chefPluginContext.chefDatabagCreator(
          action.CreateChefDatabag(context.env, s.databag, s.items, s.overwrite)
        )
      )
      case s: CreateChefRole => new ActionStepCoordinator(
        chefPluginContext.chefRoleCreator(
          action.CreateChefRole(context.env, s.role, s.description, s.runList,
            s.defaults, s.overrides, s.overwrite)
        )
      )
      case s: DestroyChefEnv => new ActionStepCoordinator(
        chefPluginContext.chefEnvDestructor(action.DestroyChefEnv(context.env))
      )
    }
  }
}
