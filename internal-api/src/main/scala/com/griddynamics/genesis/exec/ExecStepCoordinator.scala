package com.griddynamics.genesis.exec

import action.{ExecFinished, RunExecWithArgs}
import com.griddynamics.genesis.model.{VmStatus, VirtualMachine}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.{ActionResult, Signal, ActionOrientedStepCoordinator, Action}
import com.griddynamics.genesis.service.ComputeService
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}

class ExecStepCoordinator(val step: ExecRunStep, stepContext: StepExecutionContext, execPluginContext: ExecPluginContext,
                          compService: ComputeService) extends ActionOrientedStepCoordinator with Logging {
  var isStepFailed = false

  def getPubIp(vm: VirtualMachine): String = {
    compService.getIpAddresses(vm).flatMap(_.publicIp).getOrElse("unknown")
  }

  def onStepStart() = {
    import ExecNodeInitializer._
    val ip = stepContext.vms.filter(_.roleName == step.ipOfRole)
      .headOption.map(getPubIp(_)).getOrElse("")
    log.debug("public ip of role '%s' is: %s", step.ipOfRole, ip)
    for {vm <- stepContext.vms(step) if vm.status == VmStatus.Ready}
    yield RunExecWithArgs(ExecDetails(stepContext.env, vm, step.script, genesisDir, genesisDir), ip)
  }

  def onStepInterrupt(signal: Signal) = Seq()

  def onActionFinish(result: ActionResult) = result match {
    case _ if isStepFailed => {
      Seq()
    }
    case a@ExecFinished(_, _) => {
      isStepFailed = !a.isExecSuccess
      Seq()
    }
    case _ => {
      isStepFailed = true
      Seq()
    }
  }

  def getStepResult() = GenesisStepResult(stepContext.step,
    isStepFailed = isStepFailed,
    envUpdate = stepContext.envUpdate(),
    vmsUpdate = stepContext.vmsUpdate())

  def getActionExecutor(action: Action) = action match {
    case a: RunExecWithArgs => execPluginContext.syncExecRunner(a)
  }
}