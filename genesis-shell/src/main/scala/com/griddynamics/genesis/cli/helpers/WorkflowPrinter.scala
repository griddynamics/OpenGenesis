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
 */package com.griddynamics.genesis.cli.helpers

import com.griddynamics.genesis.api.GenesisService
import net.liftweb.json.{Serialization, Extraction}
import net.liftweb.json.JsonAST.{JArray, JField}

object WorkflowPrinter {
  def print(envId: Int, projectId: Int, workflowId: Int, service: GenesisService): String = {
    implicit val formats = net.liftweb.json.DefaultFormats
    val details = service.workflowHistory(envId, projectId, workflowId).getOrElse(
      throw new IllegalArgumentException(s"Failed to find workflow $workflowId for environment $envId")
    )
    val json =  Extraction.decompose(details.copy(steps = None))

    val steps = details.steps.getOrElse(Seq()).map{ step =>
      val stepId: Int = step.stepId.toInt
      var stepjs = Extraction.decompose(step)
      val stepLogs = service.getLogs(envId, stepId, false)
      if(stepLogs.nonEmpty) {
        stepjs ++= JField("logs", JArray(stepLogs.map(Extraction.decompose(_)).toList))
      }

      val actions = service.getStepLog(stepId)
      if (actions.nonEmpty) {
        val actionJS = actions.map {a =>
          val actionLogs = service.getLogs(envId, a.uuid)
          Extraction.decompose(a) ++ JField("logs", JArray(actionLogs.map(Extraction.decompose(_)).toList))
        }
        stepjs ++= JField("actions", JArray(actionJS.toList))
      }

      stepjs
    }
    val result = json ++ JField("steps", JArray(steps.toList))


    Serialization.writePretty(result)
  }
}