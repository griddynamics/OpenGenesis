package com.griddynamics.genesis.exec

import com.griddynamics.genesis.exec.action._
import com.griddynamics.genesis.model.{BorrowedMachine, EnvResource, VirtualMachine}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.{ActionResult, Signal, ActionOrientedStepCoordinator, Action}
import com.griddynamics.genesis.service.ComputeService
import com.griddynamics.genesis.plugin.StepExecutionContext
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.plugin.GenesisStepResult
import com.griddynamics.genesis.exec.action.ExecInitFail
import com.griddynamics.genesis.exec.action.RunExecWithArgs
import com.griddynamics.genesis.exec.action.ExecFinished
import com.griddynamics.genesis.exec.action.InitExecNode
import scala.collection.mutable

class ExecStepCoordinator(val step: ExecRunStep, stepContext: StepExecutionContext, execPluginContext: ExecPluginContext,
                          compService: ComputeService) extends ActionOrientedStepCoordinator with Logging {
  var isStepFailed = false

  val remoteScripts: mutable.Map[String, String] = mutable.HashMap() //actionUUID -> script

  def getPubIp(server: EnvResource): String = {
    server match {
      case vm: VirtualMachine => compService.getIpAddresses(vm).flatMap(_.publicIp).getOrElse("unknown")
      case bm: BorrowedMachine => bm.getIp.map(_.address).getOrElse("unknown")
    }

  }

  def onStepStart(): Seq[Action] = {
    val duplicates = step.commands.diff(step.commands.distinct)
    if(duplicates.nonEmpty) {
      LoggerWrapper.writeLog(stepContext.step.id, "Duplicated commands are not allowed. Following commands have duplicates: [%s]".format(duplicates.mkString(", ")))
      isStepFailed = true
      return Seq()
    }
    for {server <- stepContext.servers(step) if server.isReady }
      yield InitExecNode(stepContext.env, server)
  }

  def onStepInterrupt(signal: Signal) = Seq()

  def onActionFinish(result: ActionResult) = result match {
    case _ if isStepFailed => {
      Seq()
    }

    case ExecInitFail(a) => {
      isStepFailed = true
      Seq()
    }

    case ExecInitSuccess(a) => {
      stepContext.updateServer(a.server)
      val command = step.commands.head
      Seq(UploadScripts(stepContext.env, a.server, step.outputDirectory, command))
    }

    case ScriptsUploaded(action, server, outputPath, scriptPath) => {
      val runAction = RunExecWithArgs(ExecDetails(stepContext.env, server, scriptPath, outputPath, outputPath))
      remoteScripts(runAction.uuid) = action.script
      Seq(runAction)
    }

    case a@ExecFinished(_, _) => {
      isStepFailed = a.exitStatus.isEmpty || a.exitStatus.get != step.successExitCode

      if (!isStepFailed) {
        val executedScript = remoteScripts(a.action.uuid)
        val toBeExecuted = step.commands.dropWhile(_ != executedScript).tail
        if (toBeExecuted.nonEmpty) {
          Seq(UploadScripts(stepContext.env, a.action.execDetails.server, step.outputDirectory, toBeExecuted.head))
        } else {
          Seq()
        }
      } else {
        logFailure(a)
        Seq()
      }
    }

    case _ => {
      isStepFailed = true
      Seq()
    }
  }


  def logFailure(a: ExecFinished) {
    if (a.exitStatus.isEmpty) {
      LoggerWrapper.writeLog(stepContext.step.id, "STEP FAILURE: Process finished without returning exit code")
    } else {
      LoggerWrapper.writeLog(stepContext.step.id, "STEP FAILURE: Process finished with exit code = %d, expected success code = %d".format(a.exitStatus.get, step.successExitCode))
    }
  }

  def getStepResult() = GenesisStepResult(stepContext.step,
    isStepFailed = isStepFailed,
    envUpdate = stepContext.envUpdate(),
    serversUpdate = stepContext.serversUpdate())

  def getActionExecutor(action: Action) = action match {
    case a: InitExecNode => execPluginContext.execNodeInitializer(a)
    case a: RunExecWithArgs => execPluginContext.execRunner(a)
    case a: UploadScripts => execPluginContext.scriptsUploader(a)
  }
}