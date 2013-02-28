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
 */ package com.griddynamics.genesis.cli.commands

import com.griddynamics.genesis.api.{WorkflowDetails, ExtendedResult, WorkflowStep, ActionTracking, Configuration, GenesisService, Failure, Success}
import com.griddynamics.genesis.cli.{CreateArguments}
import java.lang.RuntimeException
import com.griddynamics.genesis.cli.helpers.{WorkflowPrinter, WorkflowTracker}
import net.liftweb.json.{Serialization, Extraction}
import net.liftweb.json.JsonAST.{JArray, JField}

class CreateCommand(service: GenesisService) {

  def execute(command: CreateArguments, projectId: Int, configuration: Configuration) {
    val env = service.createEnv(
      projectId = projectId,
      envName = command.name,
      creator = "cmd",
      templateName = command.templateName,
      templateVersion = command.templateVersion,
      variables = command.variables,
      config = configuration,
      timeToLive = None)

    env match {
      case Success(id) => {
        println("Create workflow started")

        while(service.workflowHistory(id, projectId, 0, 1).isEmpty) {}

        val workflow = service.workflowHistory(id, projectId, 0, 1).get

        val workflowId: Int = workflow.history.get.head.id

        new WorkflowTracker(service).track(
          id,
          projectId,
          workflowId,
          finished = {
            if(command.verbose) {
              println("Workflow execution details:")
              println(WorkflowPrinter.print(id, projectId, workflowId, service))
            }
          },
          succeed = {
            println(s"[SUCCESS] Environment ${command.name} was successfully created")
          },
          failed = { steps =>
            System.err.println("[FAILURE] Environment creation failed")
            Command.commonFailure(steps)
            throw new WorkflowFailureException
          }
        )


      }

      case f: Failure => throw new RuntimeException(Command.toString(f))
    }

  }
}

object Command {
  def toString(r: ExtendedResult[_]): String = {
    r match {
      case s: Success[_] => s"Success: ${s.get}"
      case f: Failure => toString(f)
    }
  }

  def toString(f: Failure): String = {
    val error = new StringBuilder()
    if (f.compoundServiceErrors.nonEmpty || f.serviceErrors.nonEmpty)
      error.append("Service errors: \n\t" + (f.compoundServiceErrors ++ f.serviceErrors.map{case (k,v) => s"$k: $v"}).reduce(_ + " \n\t" + _))
    if (f.compoundVariablesErrors.nonEmpty || f.variablesErrors.nonEmpty)
      error.append("Variable errors: \n\t" + (f.compoundVariablesErrors ++ f.variablesErrors.map{case (k,v) => s"$k: $v"}).reduce(_ + "\n\t" + _))
    error.toString()
  }

  def commonFailure(steps: Seq[(WorkflowStep, Seq[ActionTracking])]) {
    if (!steps.isEmpty) {
      for ((step, actions) <- steps) {
        println(s"\tStep '${step.stepId} - ${step.title.getOrElse(step.phase)}' - FAILED")
        actions.foreach {
          action =>
            println(s"\t\tAction ${action.uuid} - ${action.description} - FAILED")
        }
      }
    } else {
      println("No steps were executed")
    }
  }
}
