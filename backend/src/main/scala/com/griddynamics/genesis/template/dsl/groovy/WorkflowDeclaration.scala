package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.Closure
import org.codehaus.groovy.runtime.GroovyCategorySupport
import collection.mutable

class WorkflowDeclaration {
    var variablesBlock : Option[Closure[Unit]] = None
    var stepsBlock : Option[Closure[Unit]] = None
    var beforeBlock: Option[Closure[Unit]] = None
    var requirements: collection.mutable.Map[String, Closure[Boolean]] = new mutable.LinkedHashMap[String, Closure[Boolean]]

    def variables(variables : Closure[Unit]) {
        variablesBlock = Some(variables)
    }

    def steps(steps : Closure[Unit]) {
        stepsBlock = Some(steps)
    }

    def require(conditions: Closure[(String, Closure[Boolean])]) {
        val pair = GroovyCategorySupport.use(classOf[RequirementMessageCategory], conditions)
        pair._2.call()
        requirements.put(pair._1, pair._2)
    }

}

class RequirementMessageCategory{}
object RequirementMessageCategory {
   def unless(delegate: String, cl: Closure[_]) = (delegate, cl)
}

class EnvWorkflow(val name : String,
                  val variables : List[VariableDetails],
                  val stepsGenerator : Option[Closure[Unit]],
                  val preconditions: Option[Map[String, Closure[Boolean]]] = None,
                  val complete: Option[Closure[Unit]] = None)


