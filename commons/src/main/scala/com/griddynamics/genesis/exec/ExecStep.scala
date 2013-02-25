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
package com.griddynamics.genesis.exec

import reflect.BeanProperty
import collection.{JavaConversions => JC}
import java.util.{Collections, List => JList}
import com.griddynamics.genesis.plugin._
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.util.Describer

sealed trait ExecStep extends Step

case class ExecRunStep(roles: Set[String], isGlobal: Boolean, commands: IndexedSeq[String], successExitCode: Int, outputDirectory: String) extends ExecStep with RoleStep {

  override val stepDescription = new Describer("Executing shell script").param("script", commands).describe
}


class ExecRunStepBuilderFactory extends StepBuilderFactory {
  import scala.collection.JavaConversions._

  val stepName = "execRemote"

  def newStepBuilder = new StepBuilder {
    @BeanProperty var roles: JList[String] = Collections.emptyList()
    @BeanProperty var commands: JList[String] = Collections.emptyList()
    @BeanProperty var successExitCode: Int = 0
    @BeanProperty var outputDirectory: String = ExecNodeInitializer.genesisDir.toString
    @BeanProperty var isGlobal : Boolean = false

    def getDetails = ExecRunStep(roles.toSet, isGlobal, commands.toIndexedSeq, successExitCode, outputDirectory)
  }
}

class ExecStepCoordinatorFactory(execPluginContext: ExecPluginContext)
  extends PartialStepCoordinatorFactory {

  def isDefinedAt(step: Step) = step.isInstanceOf[ExecStep]

  def apply(step: Step, context: StepExecutionContext) = step match {
    case s: ExecRunStep => {
      execPluginContext.execStepCoordinator(s, context)
    }
  }

}
