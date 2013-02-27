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

import com.griddynamics.genesis.api.GenesisService
import com.griddynamics.genesis.service.StoreService
import com.griddynamics.genesis.cli.helpers.{WorkflowPrinter, WorkflowTracker}
import java.util.Date
import com.griddynamics.genesis.cli.DestroyArguments

class DestroyCommand (storeService: StoreService, service: GenesisService) {
  def execute(c: DestroyArguments, projectId: Int) {
    val env = storeService.findEnv(c.name, projectId).getOrElse(throw new IllegalArgumentException(s"Environment ${c.name} was not found"))
    val result = service.destroyEnv(env.id, projectId, Map(), "cmd")

    if (!result.isSuccess) {
      throw new RuntimeException(s"Failed to request destruction: ${Command.toString(result)}")
    }
    val workflowId = result.get
    println("Environment destruction started")

    new WorkflowTracker(service).track(env.id, projectId, workflowId,
      finished = {
        if(c.verbose) {
          println("Workflow execution details:")
          println(WorkflowPrinter.print(env.id, projectId, workflowId, service))
        }
      },
      succeed = {
        service.updateEnvironmentName(env.id, projectId, s"${c.name}_destroyed_${new Date().getTime}" )
        println(s"[SUCCESS] Environment ${c.name} was successfully destroyed")
      },
      failed = { steps =>
        System.err.println("[FAILURE] Environment destruction failed")
        Command.commonFailure(steps)
        throw new WorkflowFailureException
      }
    )
  }
}
