/**
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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.model

import org.squeryl.Schema

object GenesisSchema extends GenesisSchema with GenesisSchemaPrimitive with GenesisSchemaCustom

trait GenesisSchema extends Schema {
    val envs = table[Environment]("environment")
    val workflows = table[Workflow]("workflow")
    val vms = table[VirtualMachine]("virtual_machine")
    val steps = table[WorkflowStep]("workflow_step")

    val vmAttrs = table[SquerylEntityAttr]("vm_attribute")
    val envAttrs = table[SquerylEntityAttr]("env_attribute")
    val counters = table[NumberCounter]("number_counter")
    val logs = table[StepLogEntry]("step_logs")
  
    val projects = table[Project]("project")
    val settings = table[ConfigProperty]("settings")
}

trait GenesisSchemaPrimitive extends GenesisSchema {

    import org.squeryl.PrimitiveTypeMode._

    val envToVms = oneToManyRelation(envs, vms).via((env, vm) => env.id === vm.envId)
    envToVms.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val envsToWorkflows = oneToManyRelation(envs, workflows).via((env, workflow) => env.id === workflow.envId)
    envsToWorkflows.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val vmsToVmAttrs = oneToManyRelation(vms, vmAttrs).via((vm, attr) => vm.id === attr.entityId)
    vmsToVmAttrs.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val envsToEnvAttrs = oneToManyRelation(envs, envAttrs).via((e, attr) => e.id === attr.entityId)
    vmsToVmAttrs.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val stepsToWorkflow = oneToManyRelation(workflows, steps).via((workflow, step) => workflow.id === step.workflowId)
    stepsToWorkflow.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val envsToProject = oneToManyRelation(projects, envs).via((project, environment) => project.id === environment.projectId)
    envsToProject.foreignKeyDeclaration.constrainReference(onDelete cascade)

    on(envs)(env => declare(
        env.name is (unique)
    ))

    on(vmAttrs)(attr => declare(
        attr.value is (dbType("text"))
    ))

    on(envAttrs)(attr => declare(
        attr.value is (dbType("text"))
    ))

    on(steps)(step => declare(
        step.id is (primaryKey),
        step.details is (dbType("text"))
    ))

    on (counters) (counter => declare(
        counter.id is (primaryKey, dbType("varchar(128)"))
    ))

    on(logs) (log => declare(
      log.message is (dbType("text"))
    ))

    on(projects) (project=> declare(
      project.name is (unique),
      project.description is (dbType("text"))
    ))
}

trait GenesisSchemaCustom extends GenesisSchema {

    import org.squeryl.customtypes.CustomTypesMode._

    on(workflows)(workflow => declare(
        workflow.variables is (dbType("varchar(4096)"))
    ))
}
