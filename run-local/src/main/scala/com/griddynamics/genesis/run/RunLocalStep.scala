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

import com.griddynamics.genesis.plugin.{StepBuilder, StepBuilderFactory}
import reflect.BeanProperty
import java.util.{List => JList}
import java.util
import java.io.File
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.util.Describer

class RunLocalStepBuilderFactory extends StepBuilderFactory {
  val stepName = "execLocal"

  def newStepBuilder = new StepBuilder {
    import scala.collection.JavaConversions._

    @BeanProperty var shell: String = _
    @BeanProperty var commands: JList[String] = util.Collections.emptyList()
    @BeanProperty var runInParallel:Boolean = false
    @BeanProperty var successExitCode: Int = 0
    @BeanProperty var outputDirectory: String = _

    def getDetails = RunLocalStep(shell, commands, runInParallel, successExitCode, Option(outputDirectory).map{new File(_)})
  }
}

sealed trait RunStep extends Step

case class RunLocalStep(shell: String, commands: Seq[String], runInParallel: Boolean, successExitCode: Int, output: Option[File]) extends RunStep {
  override val stepDescription = new Describer("Shell commands execution").param("commands", commands).describe
}
