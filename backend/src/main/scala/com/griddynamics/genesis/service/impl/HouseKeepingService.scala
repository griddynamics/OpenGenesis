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
package com.griddynamics.genesis.service.impl

import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.model.{GenesisSchema => GS}
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.configuration.WorkflowContext

trait HousekeepingService {
  def markExecutingWorkflowsAsFailed()
  def allEnvsWithActiveWorkflows: List[Environment]
  def cancelAllWorkflows(envs: List[Environment])
}

class DefaultHousekeepingService extends HousekeepingService {

  @Autowired var requestBroker: WorkflowContext  = _

  @Transactional(readOnly = true)
  override def allEnvsWithActiveWorkflows =  {
    val envIDs = from (GS.workflows) (wf =>
      where ((wf.status === WorkflowStatus.Executed) or (wf.status === WorkflowStatus.Requested)) select wf.envId)
    from(GS.envs)(env => where (env.id in envIDs) select env).toList
  }

  override def cancelAllWorkflows(envs: List[Environment]) {
    envs.foreach { env => requestBroker.requestBroker.cancelWorkflow(env.id, env.projectId) }
  }

  @Transactional
  //todo: can we just rerun requested workflows?
  override def markExecutingWorkflowsAsFailed() {
    val envWithFlow = from(GS.workflows) ( wf =>
      where ( (wf.status === WorkflowStatus.Executed) or (wf.status === WorkflowStatus.Requested) )
      select(wf.envId, wf.name)
    )

    envWithFlow.iterator.foreach { case (envId, workflowName)  =>
      val status = EnvStatus.Broken
      update(GS.envs) ( env =>
        where ( env.id === envId )
          set ( env.status := status )
      )
    }
    GS.steps.update (step =>
      where ( step.status === WorkflowStepStatus.Executing )
        set ( step.status := WorkflowStepStatus.Failed )
    )
    GS.workflows.update ( wf =>
      where( (wf.status === WorkflowStatus.Executed) or (wf.status === WorkflowStatus.Requested) )
        set( wf.status := WorkflowStatus.Failed )
    )
  }
}
