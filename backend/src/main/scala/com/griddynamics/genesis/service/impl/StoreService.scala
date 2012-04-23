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
package com.griddynamics.genesis.service.impl

import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.service
import com.griddynamics.genesis.model.{GenesisSchema => GS}
import com.griddynamics.genesis.model._
import com.griddynamics.genesis.common.Mistake
import com.griddynamics.genesis.common.Mistake.throwableToLeft
import org.springframework.transaction.annotation.{Isolation, Propagation, Transactional}
import org.squeryl.{Query, Table}
import java.sql.Timestamp

//TODO think about ids as vars
class StoreService extends service.StoreService {

    import StoreService._

    @Transactional(readOnly = true)
    def listEnvs(): Seq[Environment] = from(GS.envs)(select (_)).toList

    @Transactional(readOnly = true)
    def listEnvs(projectId: Int): Seq[Environment] = listEnvQuery(projectId).toList

    @Transactional(readOnly = true)
    def countEnvs(projectId: Int) = listEnvQuery(projectId).size

    @Transactional
    def listEnvs(projectId: Int, start : Int, limit : Int) =
       listEnvQuery(projectId).page(start, limit).toList

    private def listEnvQuery(projectId: Int): Query[Environment] = {
       from(GS.envs)(env => where(env.projectId === projectId) select (env))
    }

    @Transactional(readOnly = true)
    def findEnv(envName: String) = {
        val result = from(GS.envs)(e => where(e.name === envName) select (e))
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
    def workflowsHistory(env : Environment): Seq[(Workflow, Seq[WorkflowStep])] =
        for(workflow <- listWorkflows(env)) yield
            (workflow, listWorkflowSteps(workflow))

    @Transactional(readOnly = true)
    def listWorkflowSteps(workflow : Workflow): Seq[WorkflowStep] =
        from(GS.steps)(step =>
            where(workflow.id === step.workflowId)
            select(step)
            orderBy(step.id asc)
        ).toList

    @Transactional(isolation = Isolation.SERIALIZABLE)
    def createEnv(env: Environment, workflow: Workflow) = {
        throwableToLeft {
            env.status = EnvStatus.Requested(workflow.name)
            val cenv = GS.envs.insert(env)
            updateAttrs(cenv, GS.envAttrs)

            var cworkflow = new Workflow(cenv.id, workflow.name,
                WorkflowStatus.Requested, 0, 0,
                workflow.variables, None)
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
    def createVm(vm: VirtualMachine) = {
        val cvm = GS.vms.insert(vm)
        updateAttrs(cvm, GS.vmAttrs)
        cvm
    }

    @Transactional
    def updateVm(vm: VirtualMachine) {
        GS.vms.update(vm)
        updateAttrs(vm, GS.vmAttrs)
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

    @Transactional(propagation = Propagation.MANDATORY)
    private def loadAttrs(entity: EntityWithAttrs, table: Table[SquerylEntityAttr]) = {
        val attrs = from(table)(a => where(a.entityId === entity.id) select (a)).toList

        entity.importAttrs(
            attrs.map(a => (a.name, a.value)).toMap
        )
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    def requestWorkflow(env: Environment, workflow: Workflow): Either[Mistake, (Environment, Workflow)] = {
        val actualEnv = findEnv(env.name).get

        if (!isReadyForWorkflow(actualEnv.status))
            return Left(Mistake("Environment with status %s isn't ready for workflow request".format(env.status: EnvStatus)))

        env.status = EnvStatus.Requested(workflow.name)
        workflow.status = WorkflowStatus.Requested

        updateEnv(env)
        Right((env, GS.workflows.insert(workflow)))
    }

    @Transactional(readOnly = true)
    def retrieveWorkflow(envName: String) = {
        from(GS.envs, GS.workflows)((e, w) =>
            where(e.name === envName and
                e.id === w.envId and
                w.status === WorkflowStatus.Requested)
                select ((e, w))
        ).single
    }

    @Transactional
    def startWorkflow(envName: String) = {
        val (e, w) = retrieveWorkflow(envName)

        e.status = EnvStatus.Executing(w.name)
        w.status = WorkflowStatus.Executed
        w.executionStarted = Some(new Timestamp(System.currentTimeMillis()))

        updateEnv(e)
        GS.workflows.update(w)

        (e, w, listVms(e))
    }

    @Transactional
    def finishWorkflow(env: Environment, workflow: Workflow) = {
        updateEnv(env)
        GS.workflows.update(workflow)
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
    def writeLog(stepId: Int, message: String) {
      GS.logs.insert(new StepLogEntry(stepId, message))
    }

    @Transactional
    def getLogs(stepId: Int) : Seq[StepLogEntry] = {
      from(GS.logs)((log) =>
        where(log.stepId === stepId)
        select(log)
        orderBy(log.timestamp asc)
      ).toList
    }
}

object StoreService {
    def isReadyForWorkflow(status: EnvStatus) = {
        status match {
            case EnvStatus.Requested(_) => false
            case EnvStatus.Destroyed() => false
            case EnvStatus.Executing(_) => false
            case _ => true
        }
    }
}
