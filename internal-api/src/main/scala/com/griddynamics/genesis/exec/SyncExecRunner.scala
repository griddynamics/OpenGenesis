package com.griddynamics.genesis.exec

import com.griddynamics.genesis.service.SshService
import com.griddynamics.genesis.workflow.{ActionResult, Signal, SimpleSyncActionExecutor}
import com.griddynamics.genesis.util.{InputUtil, Logging}
import org.springframework.core.io.ResourceLoader.CLASSPATH_URL_PREFIX
import com.griddynamics.genesis.exec.action.{ExecFinished, RunExecWithArgs}
import com.griddynamics.genesis.util.shell.Path
import com.griddynamics.genesis.util.shell.command.{exec, chmod}

class SyncExecRunner(val action: RunExecWithArgs, sshService: SshService) extends SimpleSyncActionExecutor with Logging {
  val SCRIPT_RESOURCE_PATH = CLASSPATH_URL_PREFIX + "shell"
  val ed = action.execDetails
  lazy val sshClient = sshService.sshClient(ed.env, ed.server)

  override def cleanUp(signal: Signal) = sshClient.disconnect

  private def getScriptContents(scriptName: String) = {
    InputUtil.locationAsString(Path(SCRIPT_RESOURCE_PATH) / scriptName)
  }

  def startSync(): ActionResult = {
    log.debug("Start sync exec with details: %s", ed)
    var resp = sshClient.exec("[ -d %s ] || mkdir %1$s".format(ed.workingDir))
    val scriptName = ed.execPath
    log.debug("script name: %s", scriptName)
    log.debug("Args: %s", action.args)
    val path = ed.workingDir / scriptName
    log.debug("script path: %s", path)
    val logPath = path + ".log"
    log.debug("log path: %s", logPath)
    sshClient.put(path, getScriptContents(scriptName))
    sshClient.exec(chmod("+x", path))
    resp = sshClient.exec(exec(path)(action.args: _*) &> logPath)
    ExecFinished(action, Option(resp.getExitCode))
  }
}
