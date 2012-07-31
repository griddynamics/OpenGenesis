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

import java.io.{FileOutputStream, File}
import com.griddynamics.genesis.util.Closeables

trait ShellExecutionStrategy  {
  def shell: String

  def generateShellCommand(command: String, outputDirectory: Option[File]): String

  def runtimeProcess: String
}


class PowerShellExecutions extends ShellExecutionStrategy {
  val shell = "powershell"

  val runtimeProcess = "powershell -nologo -executionpolicy remotesigned -command -"

  val setEncoding: String = "[Console]::OutputEncoding=[Text.Encoding]::GetEncoding(65001)\n"

  def generateShellCommand(command: String, outputDirectory: Option[File]) = {
    val withEncoding = setEncoding + command
    val scriptCall = outputDirectory.map { path =>
      val scriptPath = new File(path, "script1.ps1").getAbsolutePath
      Closeables.using(new FileOutputStream(scriptPath)) { _.write(withEncoding.getBytes) }
      "& { trap { break } . \"%s\" ; exit $LastExitCode }".format(scriptPath)
    }
    scriptCall.getOrElse(withEncoding)
  }
}

class CmdExecutionStrategy extends ShellExecutionStrategy {
  val shell = "cmd"
  val runtimeProcess = "cmd /A /K chcp 65001"

  def generateShellCommand(command: String, outputDirectory: Option[File]) = {
    def normalize(cmd: String) = if(!cmd.endsWith("\n")) cmd + "\n" else cmd

    val scriptCall = outputDirectory.map { path =>
      val scriptPath = new File(path, "script1.bat").getAbsolutePath
      Closeables.using(new FileOutputStream(scriptPath)) { _.write(command.getBytes) }
      normalize(scriptPath) + "exit %ERRORLEVEL%\n"
    }
    scriptCall.getOrElse(normalize(command))
  }
}

class ShExecutionStrategy extends ShellExecutionStrategy {
  var shell = "sh"

  var runtimeProcess = "sh"

  val newLine = System.getProperty("line.separator")

  def generateShellCommand(command: String, outputDirectory: Option[File]) = {
    val scriptCall = outputDirectory.map {path =>
      val scriptPath = new File(path, "script1.sh").getAbsolutePath
      val withHeader = "#!/bin/sh" + newLine + command
      Closeables.using(new FileOutputStream(scriptPath)) { _.write(withHeader.getBytes) }
      new File(scriptPath).setExecutable(true)
      scriptPath
    }
    scriptCall.getOrElse(command)
  }
}