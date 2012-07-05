package com.griddynamics.genesis.exec

import action.{ExecFinished, RunExecWithArgs}
import com.griddynamics.genesis.model.{BorrowedMachine, EnvResource, VmStatus, VirtualMachine}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.{ActionResult, Signal, ActionOrientedStepCoordinator, Action}
import com.griddynamics.genesis.service.ComputeService
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}

class ExecStepCoordinator(val step: ExecRunStep, stepContext: StepExecutionContext, execPluginContext: ExecPluginContext,
                          compService: ComputeService) extends ActionOrientedStepCoordinator with Logging {
  var isStepFailed = false

  def getPubIp(server: EnvResource): String = {
    server match {
      case vm: VirtualMachine => compService.getIpAddresses(vm).flatMap(_.publicIp).getOrElse("unknown")
      case bm: BorrowedMachine => bm.getIp.map(_.address).getOrElse("unknown")
    }

  }

  def onStepStart() = {
    import ExecNodeInitializer._
    val ip = stepContext.servers.filter(_.roleName == step.ipOfRole)
      .headOption.map(getPubIp(_)).getOrElse("")
    log.debug("public ip of role '%s' is: %s", step.ipOfRole, ip)
    for {server <- stepContext.servers(step) if server.isReady }
    yield RunExecWithArgs(ExecDetails(stepContext.env, server, step.script, genesisDir, genesisDir), ip)
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
    serversUpdate = stepContext.serversUpdate())

  def getActionExecutor(action: Action) = action match {
    case a: RunExecWithArgs => execPluginContext.syncExecRunner(a)
  }
}