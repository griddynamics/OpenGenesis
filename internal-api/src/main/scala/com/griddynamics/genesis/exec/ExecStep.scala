package com.griddynamics.genesis.exec

import reflect.BeanProperty
import collection.{JavaConversions => JC}
import java.util.{Collections, List => JList}
import com.griddynamics.genesis.plugin._
import com.griddynamics.genesis.workflow.Step

sealed trait ExecStep extends Step

case class ExecRunStep(roles: Set[String], ipOfRole: String,
                       script: String) extends ExecStep with RoleStep {
  def isGlobal = false
}


class ExecRunStepBuilderFactory extends StepBuilderFactory {
  val stepName = "execrun"

  def newStepBuilder = new StepBuilder {
    @BeanProperty var roles: JList[String] = Collections.emptyList()
    @BeanProperty var ipOfRole: String = _
    @BeanProperty var script: String = _

    def getDetails = ExecRunStep(JC.asScalaBuffer(roles).toSet, ipOfRole, script)
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
