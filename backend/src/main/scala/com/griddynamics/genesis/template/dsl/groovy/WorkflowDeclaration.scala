package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.{GroovyObjectSupport, Closure}
import collection.mutable
import java.util.Random
import com.griddynamics.genesis.template.DataSourceFactory

class WorkflowDeclaration(dsClozures: Option[Closure[Unit]], dataSourceFactories: Seq[DataSourceFactory], projectId: Int) {
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

    def variables = {
        val variableBuilders = variablesBlock match {
            case Some(block) => {
                val variablesDelegate = new VariableDeclaration(dsClozures, dataSourceFactories, projectId)
                block.setDelegate(variablesDelegate)
                block.setResolveStrategy(Closure.DELEGATE_FIRST)
                block.call()
                variablesDelegate.builders
            } case None => Seq[VariableBuilder]()
        }

        val vars = for(builder <- variableBuilders) yield builder.newVariable
        vars.toList
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
                  val stepsGenerator : Option[Closure[Unit]],
                  val preconditions: Map[String, Closure[Boolean]] = Map(),
                  val rescues: Option[Closure[Unit]] = None) {
    def variables(): List[VariableDetails] = List()
}


