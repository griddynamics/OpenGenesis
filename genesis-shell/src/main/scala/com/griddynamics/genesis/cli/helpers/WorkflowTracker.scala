/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.cli.helpers

import com.griddynamics.genesis.api.{ActionTracking, WorkflowDetails, WorkflowStep, GenesisService}
import com.griddynamics.genesis.model.{WorkflowStepStatus, ActionTrackingStatus, WorkflowStatus}
import scala.collection.mutable

class WorkflowTracker(service: GenesisService) {

  def track(envId: Int,
            projectId: Int,
            workflowId: Int,
            finished: => Unit = {},
            succeed: => Unit = {},
            failed: (Seq[(WorkflowStep, Seq[ActionTracking])]) => Unit = { _ => }) {
    def pollWorkflowState: WorkflowDetails = service.workflowHistory(envId, projectId, workflowId).getOrElse(
      throw new IllegalArgumentException(s"Failed to find workflow $workflowId for environment ${envId}")
    )

    var history = pollWorkflowState

    val reportedSteps = mutable.HashSet[String]()
    val reportedActions = mutable.HashSet[String]()

    while(!isFinished(history)) {
      for (steps <- history.steps) {
        val done = steps.filter(s => isFinished(s) && !reportedSteps.contains(s.stepId))
        done.foreach {s =>
          println(s"> Step '${s.title.getOrElse(s.phase)}' finished. ${s.status}")
        }

        val executing = steps.filter(_.status == WorkflowStepStatus.Executing.toString)
        executing.foreach { s =>
          val actions = service.getStepLog(s.stepId.toInt)
          val finished = actions.filter(a => isFinished(a) && !reportedActions.contains(a.uuid))
          finished.foreach(a =>
            println(s"> Action '${a.name}' finished. ${a.status}")
          )
          reportedActions ++= finished.map(_.uuid)
        }

        reportedSteps ++= done.map(_.stepId)
      }
      history = pollWorkflowState
    }

    finished

    if(history.status == WorkflowStatus.Succeed.toString) {
      succeed
    } else {
      val failedSteps = history.steps.getOrElse(Seq()).filter(_.status == WorkflowStepStatus.Failed.toString)
      val result = failedSteps.map { failedStep =>
        val failedActions = service.getStepLog(failedStep.stepId.toInt).filter(_.status == ActionTrackingStatus.Failed.toString)
        (failedStep, failedActions)
      }
      failed(result)
    }
  }

  private def isFinished(action: ActionTracking) = action.status != ActionTrackingStatus.Executing.toString
  private def isFinished(step: WorkflowStep) = step.status != WorkflowStepStatus.Executing.toString && step.status != WorkflowStepStatus.Requested.toString
  private def isFinished(w: WorkflowDetails)  = w.status != WorkflowStatus.Executing.toString && w.status != WorkflowStatus.Requested.toString
}
