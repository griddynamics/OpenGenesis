/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.run

import com.griddynamics.genesis.util.Logging
import java.io.{File, InputStream, PrintWriter}
import io.Source
import com.griddynamics.genesis.logging.LoggerWrapper
import com.griddynamics.genesis.util.TryingUtil._

class LocalShellExecutionService(executionStrategies: List[ShellExecutionStrategy]) extends Logging {
  import scala.sys.process._

  val strategies = executionStrategies.map { strategy => (strategy.shell, strategy) }.toMap

  private[this] def handleInputStream (logWriter: Option[PrintWriter], actionUUID: Option[String]) (is: InputStream, sb: StringBuilder) {
    Source.fromInputStream(is, "UTF-8").getLines().foreach { line =>
      log.debug(line)
      logWriter.foreach { _.println(line) }
      actionUUID.foreach { LoggerWrapper.writeLog(_, line) }
      sb.append(line).append('\n')
    }
  }

  def exec(shell: String, command: String, outPath : Option[File] = None, actionUUID : Option[String] = None): ExecResponse = {
    val strategy = strategies.getOrElse(shell, throw new IllegalArgumentException)

    log.debug("Executing command: %s, log output path: %s, shell-run command: %s", command, outPath, shell)

    outPath.foreach { path =>
      if(!path.exists && !path.mkdirs()) {
        actionUUID.foreach(LoggerWrapper.writeLog(_, "Couldn't create directory [%s]".format(path.getAbsolutePath)))
        throw new IllegalStateException("Failed to create %s".format(path))
      }
    }
    val processCmd = strategy.generateShellCommand(command, outPath)

    val outputWriter = outPath.map { it => new PrintWriter(new File(it, "exec.out")) }
    val handleOutput = handleInputStream(outputWriter, actionUUID) _

    val errorWriter = outPath.map { it => new PrintWriter(new File(it, "exec.err")) }
    val handleError = handleInputStream(errorWriter, actionUUID) _

    val exitCodeWriter = outPath.map { it => new PrintWriter(new File(it, "exec.status")) }

    try {
      val out, err = new StringBuilder

      val pio = new ProcessIO({in => in.write(processCmd.getBytes); in.close()}, handleOutput(_, out), handleError(_, err))

      val process = strategy.runtimeProcess.run(pio)
      log.debug("Process was started: %s", process)

      val exitCode = process.exitValue()
      exitCodeWriter.map {_.write(exitCode.toString)}
      ExecResponse(exitCode = exitCode, out = out.toString(), err = err.toString())

    } finally {
      Seq(outputWriter, errorWriter, exitCodeWriter).flatten.map {it => silently { it.close() }}
    }
  }
}

case class ExecResponse(out: String, err: String = "",  exitCode: Int = 0)
