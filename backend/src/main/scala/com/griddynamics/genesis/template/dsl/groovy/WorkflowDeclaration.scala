package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.{GroovyObjectSupport, Closure}
import collection.mutable
import java.util.Random

class WorkflowDeclaration {
    var variablesBlock : Option[Closure[Unit]] = None
    var stepsBlock : Option[Closure[Unit]] = None
    var rescueBlock: Option[Closure[Unit]] = None
    var requirements: collection.mutable.Map[String, Closure[Boolean]] = new mutable.LinkedHashMap[String, Closure[Boolean]]

    def variables(variables : Closure[Unit]) {
        variablesBlock = Some(variables)
    }

    def steps(steps : Closure[Unit]) {
        stepsBlock = Some(steps)
    }

    def require(conditions: Closure[(String, Closure[Boolean])]) {
        val handler: RequirementsHandler = new RequirementsHandler
        conditions.setDelegate(handler)
        conditions.call()
        requirements = handler.requirementsMap
    }

    def onError(rescBlock: Closure[Unit]) {
        rescueBlock = Some(rescBlock)
    }
}

class RequirementsHandler extends GroovyObjectSupport {
    val requirementsMap: mutable.Map[String, Closure[Boolean]] = mutable.Map()

    override def invokeMethod(name: String, args: AnyRef) : AnyRef = {
       val arguments = args.asInstanceOf[Array[AnyRef]]
       requirementsMap.put(name, arguments(0).asInstanceOf[Closure[Boolean]])
       false.asInstanceOf[AnyRef]
    }
}

class EnvWorkflow(val name : String,
                  val variables : List[VariableDetails],
                  val stepsGenerator : Option[Closure[Unit]],
                  val preconditions: Map[String, Closure[Boolean]] = Map(),
                  val rescues: Option[Closure[Unit]] = None)


