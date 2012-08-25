package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.Closure

class WorkflowDeclaration {
    var variablesBlock : Option[Closure[Unit]] = None
    var stepsBlock : Option[Closure[Unit]] = None
    var beforeBlock: Option[Closure[Unit]] = None

    def variables(variables : Closure[Unit]) {
        variablesBlock = Some(variables)
    }

    def steps(steps : Closure[Unit]) {
        stepsBlock = Some(steps)
    }

    def require(requirements: Closure[java.util.Map[String, Closure[Boolean]]]) {
        import collection.JavaConversions._
        val call = mapAsScalaMap(requirements.call)
        call.toMap.foreach(e => e._2.call())
    }

}

class EnvWorkflow(val name : String,
                  val variables : List[VariableDetails],
                  val stepsGenerator : Option[Closure[Unit]],
                  val complete: Option[Closure[Unit]] = None)

