package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.Closure

class WorkflowDeclaration {
    var variablesBlock : Option[Closure[Unit]] = None
    var stepsBlock : Option[Closure[Unit]] = None

    def variables(variables : Closure[Unit]) {
        variablesBlock = Some(variables)
    }

    def steps(steps : Closure[Unit]) {
        stepsBlock = Some(steps)
    }
}

class EnvWorkflow(val name : String,
                  val variables : List[VariableDetails],
                  val stepsGenerator : Option[Closure[Unit]])

