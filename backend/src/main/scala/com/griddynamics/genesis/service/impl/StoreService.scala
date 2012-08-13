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
package com.griddynamics.genesis.service.impl

import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.service
import com.griddynamics.genesis.model.{GenesisSchema => GS}
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.model.EnvStatus._
import com.griddynamics.genesis.common.Mistake
import com.griddynamics.genesis.common.Mistake.throwableToLeft
import org.springframework.transaction.annotation.{Isolation, Propagation, Transactional}
import org.squeryl.{StaleUpdateException, Query, Table}
import java.sql.Timestamp
import com.griddynamics.genesis.model.WorkflowStepStatus._
import com.griddynamics.genesis.util.Logging

//TODO think about ids as vars
class StoreService extends service.StoreService with Logging {

  import StoreService._

  val availableEnvs = from(GS.envs, GS.projects) { (env, project) =>
    where(env.projectId === project.id and project.isDeleted === false) select(env)
  }

  @Transactional(readOnly = true)
  def listEnvs(projectId: Int): Seq[Environment] = listEnvQuery(projectId).toList

  @Transactional
  def listEnvs(projectId: Int, start : Int, limit : Int) =
    listEnvQuery(projectId).page(start, limit).toList

  @Transactional(readOnly = true)
  def listEnvs(projectId: Int, statuses: Seq[EnvStatus]): Seq[Environment] = {
    from(GS.envs)(env =>
      where(env.status.id in statuses.map(_.id) and env.projectId === projectId)
      select (env)
    ).toList
  }

  @Transactional(readOnly = true)
  def countEnvs(projectId: Int) = listEnvQuery(projectId).size

  @Transactional(readOnly = true)
  def countEnvs(projectId: Int, statuses: Seq[EnvStatus]) = listEnvs(projectId, statuses).size

  private def listEnvQuery(projectId: Int): Query[Environment] = {
    from(GS.envs)(env => where(env.projectId === projectId) select (env))
  }

  @Transactional(readOnly = true)
  def isEnvExist(projectId: Int, envId: Int): Boolean = {
    !from(GS.envs)(env => where(env.projectId === projectId and env.id === envId) select (env.id)).headOption.isEmpty
  }

  private def findEnvQuery(envId: Int, projectId: Int) =
    from(GS.envs)(e => where(e.id === envId and e.projectId === projectId) select (e))

    private def findEnvQuery(envName: String, projectId: Int) =
        from(GS.envs)(e => where(e.name === envName and e.projectId === projectId) select (e))

  @Transactional(readOnly = true)
  def findEnv(envId: Int, projectId: Int) = {
    val result = findEnvQuery(envId, projectId)
    val envs = if (result.size == 1) Some(result.single) else None
    envs.foreach(loadAttrs(_, GS.envAttrs))
    envs
  }

    @Transactional(readOnly = true)
    def findEnv(envName: String, projectId: Int) = {
        val result = findEnvQuery(envName, projectId)
        val envs = if (result.size == 1) Some(result.single) else None
        envs.foreach(loadAttrs(_, GS.envAttrs))
        envs
    }

  //TODO switch to join query
  @Transactional(readOnly = true)
  def findEnvWithWorkflow(envId: Int, projectId: Int) = {
    findEnv(envId).map( env =>
      (env, listWorkflows(env).find(_.status == WorkflowStatus.Executed))
    )
  }

  @Transactional(readOnly = true)
  def findEnv(id: Int) = {
    val result = from(GS.envs)(e => where(e.id === id) select (e))
    val envs = if (result.size == 1) Some(result.single) else None
    envs.foreach(loadAttrs(_, GS.envAttrs))
    envs
  }

  @Transactional(readOnly = true)
  def getVm(instanceId: String) = {
    from(GS.envs, GS.vms)((env, vm) =>
      where(vm.envId === env.id and
          vm.instanceId === Some(instanceId))
          select ((env, vm))
    ).single
  }

  @Transactional(readOnly = true)
  def listServers(env: Environment): Seq[BorrowedMachine] = {
    val vms = from(GS.borrowedMachines)(server => where(server.envId === env.id) select (server)).toList
    vms.foreach(loadAttrs(_, GS.serverAttrs))
    vms
  }

  @Transactional(readOnly = true)
  def listVms(env: Environment): Seq[VirtualMachine] = {
    val vms = from(GS.vms)(vm => where(vm.envId === env.id) select (vm)).toList
    vms.foreach(loadAttrs(_, GS.vmAttrs))
    vms
  }

  @Transactional(readOnly = true)
  def listWorkflows(env: Environment) = {
    from(GS.workflows)(w =>
      where(w.envId === env.id)
          select (w)
          orderBy(w.id desc)
    ).toList
  }

  @Transactional(readOnly = true)
  def listWorkflows(env: Environment, pageOffset: Int, pageLength: Int) = {
    from(GS.workflows)(w =>
      where(w.envId === env.id)
          select (w)
          orderBy(w.id desc)
    ).page(pageOffset, pageLength).toList
  }

  @Transactional(readOnly = true)
  def countWorkflows(env: Environment): Int =
    from(GS.workflows)(w =>
      where(w.envId === env.id)
          compute(count)
    ).single.measures.toInt

  //TODO switch to join query
  @Transactional(readOnly = true)
  def listEnvsWithWorkflow(projectId: Int) = {
    listEnvs(projectId).map(env => {
      (env, listWorkflows(env).find(w => w.status == WorkflowStatus.Executed))
    })
    //join(envs, workflows.leftOuter)((env, w) => {
    //    on(env.id === w.map(_.envId))
    //    where(w.map(_.status) === WorkflowStatus.Executed)
    //    select(env, w)
    //}).toList
  }

  @Transactional(readOnly = true)
  def listEnvsWithWorkflow(projectId: Int, statuses: Seq[EnvStatus]) = {
    listEnvs(projectId, statuses).map(env => {
      (env, listWorkflows(env).find(w => w.status == WorkflowStatus.Executed))
    })
  }

  @Transactional(readOnly = true)
  def listEnvsWithWorkflow(projectId: Int, start : Int, limit : Int) = {
    listEnvs(projectId, start, limit).map(env => {
      (env, listWorkflows(env).find(w => w.status == WorkflowStatus.Executed))
    })
    //join(envs, workflows.leftOuter)((env, w) => {
    //    on(env.id === w.map(_.envId))
    //    where(w.map(_.status) === WorkflowStatus.Executed)
    //    select(env, w)
    //}).toList
  }

  @Transactional(readOnly = true)
  def workflowsHistory(env : Environment, pageOffset: Int, pageLength: Int): Seq[(Workflow, Seq[WorkflowStep])] =
    for(workflow <- listWorkflows(env, pageOffset, pageLength)) yield
      (workflow, listWorkflowSteps(workflow))

  @Transactional(readOnly = true)
  def listWorkflowSteps(workflow : Workflow): Seq[WorkflowStep] =
    from(GS.steps)(step =>
      where(workflow.id === step.workflowId)
          select(step)
          orderBy(step.id asc)
    ).toList

  @Transactional
  def createEnv(env: Environment, workflow: Workflow) = {
    throwableToLeft {
      env.status = EnvStatus.Busy
      val cenv = GS.envs.insert(env)
      updateAttrs(cenv, GS.envAttrs)

      var cworkflow = new Workflow(cenv.id, workflow.name, workflow.startedBy,
        WorkflowStatus.Requested, 0, 0,
        workflow.variables, None, None)
      cworkflow = GS.workflows.insert(cworkflow)

      (cenv, cworkflow)
    }
  }

  @Transactional
  def updateEnv(env: Environment) {
    GS.envs.update(env)
    updateAttrs(env, GS.envAttrs)
  }

  @Transactional
  def resetEnvStatus(env: Environment) : Option[Mistake] = {
    lazy val mistake = Some(Mistake("Can't reset status of environment [%s] because it's not in 'Broken' state".format(env.name)))

    env.status match {
      case EnvStatus.Broken => {
        val updatedRowCount = update(GS.envs)(e =>
          where(e.id === env.id and
                e.status === EnvStatus.Broken)
          set(e.status := EnvStatus.Ready)
        )
        if (updatedRowCount != 1) mistake else None
      }
      case _ => mistake
    }
  }

  @Transactional
  def createVm(vm: VirtualMachine) = {
    val cvm = GS.vms.insert(vm)
    updateAttrs(cvm, GS.vmAttrs)
    cvm
  }

  @Transactional
  def updateServer(resource: EnvResource) {
    resource match {
      case vm: VirtualMachine => {
        GS.vms.update(vm)
        updateAttrs(vm, GS.vmAttrs)
      }
      case server: BorrowedMachine => {
        GS.borrowedMachines.update(server)
        updateAttrs(server, GS.serverAttrs)
      }
    }
  }

  @Transactional
  def createBM(bm: BorrowedMachine) = {
    val cbm = GS.borrowedMachines.insert(bm)
    updateAttrs(cbm, GS.serverAttrs)
    cbm
  }

  @Transactional(propagation = Propagation.MANDATORY)
  private def updateAttrs(entity: EntityWithAttrs, table: Table[SquerylEntityAttr]) = {
    val (changedAttrs, removedAttrs) = entity.exportAttrs()

    if (!removedAttrs.isEmpty)
      table.deleteWhere(a => a.name in removedAttrs and a.entityId === entity.id)

    if (!changedAttrs.isEmpty) {
      table.deleteWhere(a => a.name in changedAttrs.keys and a.entityId === entity.id)

      table.insert(for ((name, value) <- changedAttrs) yield
        new SquerylEntityAttr(entity.id, name, value)
      )
    }
  }

  @Transactional
  def finishWorkflow(env: Environment, workflow: Workflow) {
    val finishTime: Some[Timestamp] = Some(new Timestamp(System.currentTimeMillis()))

    GS.steps.update(step =>
      where((step.finished isNull) and (step.workflowId === workflow.id) and (step.started isNotNull)) set (
          step.finished := finishTime
          )
    )
    updateEnv(env)

    workflow.executionFinished = finishTime
    GS.workflows.update(workflow)
  }

  @Transactional(propagation = Propagation.MANDATORY)
  private def loadAttrs(entity: EntityWithAttrs, table: Table[SquerylEntityAttr]) = {
    val attrs = from(table)(a => where(a.entityId === entity.id) select (a)).toList

    entity.importAttrs(
      attrs.map(a => (a.name, a.value)).toMap
    )
  }

  @Transactional
  def requestWorkflow(env: Environment, workflow: Workflow): Either[Mistake, (Environment, Workflow)] = {
    val actualEnv = findEnv(env.name, env.projectId).get

    if (!isReadyForWorkflow(actualEnv.status))
      return Left(Mistake("Environment with status %s isn't ready for workflow request".format(actualEnv.status: EnvStatus)))

    actualEnv.status = EnvStatus.Busy
    workflow.status = WorkflowStatus.Requested
    try {
      updateEnv(actualEnv)
      Right((actualEnv, GS.workflows.insert(workflow)))
    } catch {
      case e: StaleUpdateException => {
        log.warn("Optimistic lock: environment's status has been updated", e)
        Left(Mistake("Optimistic lock: environment's status has been updated"))
      }
    }
  }

  @Transactional(readOnly = true)
  def retrieveWorkflow(envId: Int, projectId: Int) = {
    from(GS.envs, GS.workflows)((e, w) =>
      where(e.id === envId and
          e.projectId === projectId and
          e.id === w.envId and
          w.status === WorkflowStatus.Requested)
          select ((e, w))
    ).single
  }

  @Transactional
  def startWorkflow(envId: Int, projectId: Int) = {
    val (e, w) = retrieveWorkflow(envId, projectId)
    loadAttrs(e, GS.envAttrs)
    e.status = EnvStatus.Busy
    w.status = WorkflowStatus.Executed
    w.executionStarted = Some(new Timestamp(System.currentTimeMillis()))

    updateEnv(e)
    GS.workflows.update(w)

    (e, w, listVms(e) ++ listServers(e))
  }

  @Transactional
  def updateWorkflow(w: Workflow) {
    GS.workflows.update(w)
  }

  @Transactional
  def updateStep(step: WorkflowStep) {
    GS.steps.update(step)
  }

  @Transactional
  def updateStepStatus(stepId: Int, status: WorkflowStepStatus) {
    updateStepDetailsAndStatus(stepId, None, status)
  }


  @Transactional
  def updateStepDetailsAndStatus(stepId: Int, details: Option[String], status: WorkflowStepStatus.WorkflowStepStatus) {
    import com.griddynamics.genesis.model.WorkflowStepStatus._
    GS.steps.update(step => {
      where(step.id === stepId) set (
        step.finished := (if(status == Failed || status == Succeed) Some(new Timestamp(System.currentTimeMillis())) else step.finished),
        step.started := (if(status == Executing) Some(new Timestamp(System.currentTimeMillis())) else step.started),
        step.status := status,
        step.details := details.getOrElse(step.details)
      )
    })
  }

  @Transactional
  def insertWorkflowSteps(steps : Seq[WorkflowStep]) =
    for(step <- steps) yield insertWorkflowStep(step)

  @Transactional
  def insertWorkflowStep(step : WorkflowStep) = GS.steps.insert(step)

  @Transactional
  def allocateStepCounters(count : Int = 1) = {
    val key: String = WorkflowStep.getClass.getSimpleName
    val forUpdate: Query[NumberCounter] = from(GS.counters)(counter => {
      (where(counter.id === key) select (counter))
    }).forUpdate
    val newCounter : NumberCounter = forUpdate.headOption.getOrElse(new NumberCounter(key))
    newCounter.increment(count)
    val last = from(GS.steps)(s => (compute(max(s.id)))).getOrElse(0).asInstanceOf[Int]
    if (last >= newCounter.counter - count) {
      newCounter.counter = last  + count + 1
    }
    GS.counters.insertOrUpdate(newCounter)
    newCounter.counter - count
  }

  @Transactional
  def writeLog(stepId: Int, message: String, timestamp: Timestamp = new Timestamp(System.currentTimeMillis())) {
    GS.logs.insert(new StepLogEntry(stepId, message, timestamp))
  }

  @Transactional
  def writeActionLog(actionUUID: String, message: String, timestamp: Timestamp = new Timestamp(System.currentTimeMillis())) {
    val stepID = from(GS.actionTracking)((action) => where (action.actionUUID === actionUUID) select(action.workflowStepId)).headOption
    stepID.foreach { id => GS.logs.insert(new StepLogEntry(id, message, timestamp, Option(actionUUID))) }
  }

  @Transactional
  def getLogs(stepId: Int) : Seq[StepLogEntry] = {
    from(GS.logs)((log) =>
      where(log.stepId === stepId)
          select(log)
          orderBy(log.timestamp asc)
    ).toList
  }

  @Transactional
  def getLogs(actionUUID: String) = {
    from(GS.logs)((log) =>
      where(log.actionUUID === Option(actionUUID))
          select(log)
          orderBy(log.timestamp asc)
    ).toList
  }

  @Transactional
  def startAction(actionTracking: ActionTracking) = {
    GS.actionTracking.insert(actionTracking)
  }

  @Transactional
  def endAction(uuid: String, message: Option[String], status: ActionTrackingStatus.ActionStatus) {
    GS.actionTracking.update(at => {
      where(at.actionUUID === uuid) set (
          at.finished := Some(new Timestamp(System.currentTimeMillis())),
          at.description := message,
          at.status := status
          )
    })
  }

  @Transactional
  def cancelRunningActions(stepId: Int) {
    GS.actionTracking.update( at => {
      where(at.workflowStepId === stepId and at.finished === None) set (
          at.status := ActionTrackingStatus.Canceled,
          at.finished := Some(new Timestamp(System.currentTimeMillis()))
          )
    })
  }

  @Transactional
  def getActionLog(stepId : Int) = {
    from(GS.actionTracking)(at => where(at.workflowStepId === stepId) select (at) orderBy(at.started asc)).toList
  }

  def findBorrowedMachinesByServerId(serverId: Int) = {
    from(GS.borrowedMachines)(bm => where(bm.serverId === serverId and bm.status <> MachineStatus.Released) select (bm)).toList
  }
}

object StoreService {
    def isReadyForWorkflow(status: EnvStatus) = {
        status match {
            case EnvStatus.Busy => false
            case EnvStatus.Destroyed => false
            case _ => true
        }
    }
}
