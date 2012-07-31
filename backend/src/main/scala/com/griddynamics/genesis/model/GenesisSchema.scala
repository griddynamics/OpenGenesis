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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.model

import org.squeryl.Schema
import security.GenesisAclSchema

object GenesisSchema extends GenesisSchema with GenesisSchemaPrimitive with GenesisSchemaCustom with GenesisAclSchema

trait GenesisSchema extends Schema {
    val envs = table[Environment]("environment")
    val workflows = table[Workflow]("workflow")
    val vms = table[VirtualMachine]("virtual_machine")

    val borrowedMachines = table[BorrowedMachine]("borrowed_machine")
    val serverAttrs = table[SquerylEntityAttr]("borrowed_machine_attrs")

    val steps = table[WorkflowStep]("workflow_step")

    val vmAttrs = table[SquerylEntityAttr]("vm_attribute")
    val envAttrs = table[SquerylEntityAttr]("env_attribute")
    val counters = table[NumberCounter]("number_counter")
    val logs = table[StepLogEntry]("step_logs")
  
    val projects = table[Project]("project")
    val settings = table[ConfigProperty]("settings")
    val credentials = table[Credentials]("credentials")

    val userAuthorities = table[Authority]("user_authorities")
    val groupAuthorities = table[Authority]("group_authorities")

    val serverArrays = table[ServerArray]("server_array")
    val servers = table[Server]("predefined_server")

    val actionTracking = table[ActionTracking]("action_tracking")

    val dataBags = table[DataBag]("databag")
    val dataBagItems = table[DataBagItem]("databag_item")
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

    val serverArrayToProject = oneToManyRelation(projects, serverArrays).via((project, array) => project.id === array.projectId)
    serverArrayToProject.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val serverToServerArray = oneToManyRelation(serverArrays, servers).via((array, server) => array.id === server.serverArrayId)
    serverToServerArray.foreignKeyDeclaration.constrainReference(onDelete cascade)

    val itemToDatabag = oneToManyRelation(dataBags, dataBagItems).via((bag, item) => bag.id === item.dataBagId)
    itemToDatabag.foreignKeyDeclaration.constrainReference(onDelete cascade)

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
      log.message is (dbType("text")),
      log.actionUUID is dbType("varchar(36)"),
      columns(log.actionUUID) are (indexed("logs_action_uuid_idx"))
    ))

    on(projects) (project=> declare(
      project.name is (unique),
      project.description is (dbType("text")),
      project.isDeleted defaultsTo(false)
    ))

    on(envs) (env => declare(
      columns(env.name, env.projectId) are (unique)
    ))

    on(credentials)(creds => declare(
      creds.id is (primaryKey, autoIncremented),
      columns(creds.cloudProvider, creds.pairName, creds.projectId) are (unique),
      creds.credential is (dbType("text"))
    ))

    on(userAuthorities)(authority => declare(
      columns(authority.principalName, authority.authority) are (unique)
    ))

    on(groupAuthorities)(authority => declare(
      columns(authority.principalName, authority.authority) are (unique)
    ))

    on(actionTracking)(tracking => declare(
        tracking.description is dbType("text"),
        tracking.actionName is dbType("varchar(256)"),
        tracking.actionUUID is dbType("varchar(36)"),
        columns(tracking.workflowStepId) are (indexed("step_idx")),
        columns(tracking.actionUUID) are (indexed("action_uuid_idx"))
    ))

    on(serverArrays)(array => declare (
      array.id is (primaryKey, autoIncremented),
      array.name is dbType("varchar(128)"),
      array.description is dbType("varchar(128)"),
      columns(array.projectId, array.name) are (unique)
    ))

    on(servers)(server => declare (
      server.id is (primaryKey, autoIncremented),
      server.instanceId is dbType("varchar(128)"),
      columns(server.serverArrayId, server.instanceId) are (unique)
    ))

    on(serverAttrs)(attr => declare (
      attr.value is (dbType("text"))
    ))

    on(dataBags)(bag => declare (
      bag.id is (primaryKey, autoIncremented),
      bag.name is (dbType("varchar(128)")),
      bag.tags is (dbType("varchar(512)")),
      bag.projectId is (dbType("int")),
      columns(bag.name, bag.projectId) are (unique)
    ))

    on(dataBagItems)(item => declare (
      item.id is (primaryKey, autoIncremented),
      columns(item.dataBagId, item.itemKey) are (unique),
      item.itemKey is (dbType("varchar(256)")),
      item.itemValue is (dbType("varchar(256)"))
    ))
}

trait GenesisSchemaCustom extends GenesisSchema {
    import org.squeryl.customtypes.CustomTypesMode._
    on(workflows)(workflow => declare(
        workflow.variables is (dbType("varchar(4096)"))
    ))
}
