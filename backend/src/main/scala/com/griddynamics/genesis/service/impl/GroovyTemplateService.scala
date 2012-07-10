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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.service.impl

import collection.mutable
import com.griddynamics.genesis.service
import groovy.lang._
import collection.mutable.ListBuffer
import service._
import org.springframework.core.convert.ConversionService
import java.lang.IllegalStateException
import com.griddynamics.genesis.template.dsl.groovy._
import com.griddynamics.genesis.template._
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.plugin.{StepBuilder, StepBuilderFactory}
import com.griddynamics.genesis.util.{ScalaUtils, Logging}
import org.codehaus.groovy.runtime.{InvokerHelper, MethodClosure}
import scala.Some
import service.ValidationError
import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.repository.{DatabagRepository, ProjectPropertyRepository}
import groovy.util.Expando

class GroovyTemplateService(val templateRepository : TemplateRepository,
                            val stepBuilderFactories : Seq[StepBuilderFactory],
                            val conversionService : ConversionService,
                            val dataSourceFactories : Seq[DataSourceFactory] = Seq(),
                            val projectPropertyRepository: ProjectPropertyRepository,
                            databagRepository: DatabagRepository )
    extends service.TemplateService with Logging {

    def findTemplate(projectId: Int, name: String, version: String) = getTemplate(projectId, name, version)

    def getTemplate(projectId: Int, name: String, version: String, eval: Boolean = true) : Option[TemplateDefinition] = {
        val body = templatesMap(projectId).get(name, version)
        body.flatMap(evaluateTemplate(projectId, _, None, None, None, listOnly = !eval).map(et =>
            new GroovyTemplateDefinition(et, conversionService, stepBuilderFactories, projectPropertyRepository, projectId)
        ))
    }

    override def descTemplate(projectId: Int, name: String, version: String) = getTemplate(projectId, name, version, eval = false)

    def listTemplates(projectId: Int) = templatesMap(projectId).keys.toSeq

    private def templatesMap(projectId: Int) = {
        val sources = templateRepository.listSources()
        (for ((version, body) <- sources) yield try {
            val template = evaluateTemplate(projectId, body, None, None, None, true)
            template.map(t => ((t.name, t.version), body))
       } catch {
            case t: Throwable => {
                log.error(t, "Error in template name or version: %s", t)
                None
            }
        }).flatten.toMap        
    }

    def evaluateTemplate(projectId: Int, body : String, extName: Option[String], extVersion: Option[String],
                         extProject : Option[String], listOnly : Boolean = false) = {
        val templateDecl = new BlockDeclaration
        val methodClosure = new MethodClosure(templateDecl, "declare")

        val binding = new Binding
        binding.setVariable("template", methodClosure)

        try {
            new GroovyShell(binding).evaluate(body)
        } catch {
            case e: GroovyRuntimeException => throw new IllegalStateException("can't process template", e)
        }

        val templateBuilder = if (listOnly) new NameVersionDelegate else new EnvTemplateBuilder(projectId, dataSourceFactories, databagRepository, projectPropertyRepository)
        templateDecl.bodies.headOption.map {
            templateBody => {
                templateBody.setDelegate(templateBuilder)
                templateBody.call()
                templateBuilder.newTemplate(extName, extVersion, extProject)
            }
        }
    }

    def templateRawContent(projectId: Int, name: String, version: String) = {
        val map = templatesMap(projectId)
        map.get(name, version)
    }
}

class StepBodiesCollector(variables: Map[String, AnyRef],
                          stepBuilderFactories : Seq[StepBuilderFactory],
                           override val repository: ProjectPropertyRepository,
                           override val projectId: Int)
    extends GroovyObjectSupport with ProjectContextFromProperties {

    val closures = (for (factory <- stepBuilderFactories) yield {
        (factory.stepName, (ListBuffer[Closure[Unit]](), factory))
    }).toMap


    def $(evalExpression: String) = {
     new ContextAccess {
       def apply(context: scala.collection.Map[String, Any]) = {

         val binding = new Binding()
         context.foreach { case (key, value) => binding.setVariable(key, value) }
         binding.setVariable("project", getProject)
         val shell = new GroovyShell(binding)
         shell.evaluate(evalExpression)
       }
     }
    }

    override def invokeMethod(name: String, args: AnyRef) = {
        val opt = closures.get(name)
        if (opt.isEmpty) super.invokeMethod(name, args)

        for ((bodies, _) <- opt) {
            bodies += args.asInstanceOf[Array[AnyRef]].head.asInstanceOf[Closure[Unit]]
        }

        null
    }

    override def getProperty(property: String) = {
        variables.getOrElse(property, super.getProperty(property))
    }

  def buildSteps = {
        (for ((bodies, factory) <- closures.values) yield {
            for (body <- bodies) yield {
                val stepBuilder = new StepBuilderProxy(factory.newStepBuilder, getProject)
                body.setDelegate(stepBuilder)
                body.setResolveStrategy(Closure.DELEGATE_FIRST)
                body.call()
                stepBuilder
            }
        }).flatten
    }
}

trait ContextAccess extends ((scala.collection.Map[String, Any]) => Any)

object UninitializedStepDetails extends Step {
    override def stepDescription = "..."
}

class StepBuilderProxy(stepBuilder: StepBuilder, project: ProjectContextSupport) extends GroovyObjectSupport with StepBuilder {
    private val contextDependentProperties = mutable.Map[String, ContextAccess]()
    def getDetails = if(contextDependentProperties.isEmpty) stepBuilder.getDetails else UninitializedStepDetails

    def newStep(context: scala.collection.Map[String, AnyRef]): GenesisStep = {
        contextDependentProperties.foreach { case (propertyName, contextAccess) =>
            ScalaUtils.setProperty(stepBuilder, propertyName, contextAccess(context))
        }
        stepBuilder.id = this.id
        stepBuilder.phase = this.phase
        stepBuilder.exportTo = this.exportTo
        stepBuilder.ignoreFail = this.ignoreFail
        stepBuilder.precedingPhases = this.precedingPhases
        stepBuilder.newStep
    }

    override def newStep = newStep(Map())


    override def setProperty(property: String, value: Any) {
        value match {
          case value: Closure[_] =>
            contextDependentProperties(property) = new ContextAccess {
              def apply(v1: collection.Map[String, Any]) = {
                import scala.collection.JavaConversions._
                value.setDelegate(new Expando(v1))
                value.call()
              }
            }
          case value: ContextAccess =>
            contextDependentProperties(property) = value
          case _ => {
            ScalaUtils.setProperty(stepBuilder, property, value)
            if (ScalaUtils.hasProperty(this, property, ScalaUtils.getType(value))) {
              ScalaUtils.setProperty(this, property, value)
            }
          }
        }
    }

    override def getProperty(property: String) =  {
        InvokerHelper.getProperty(stepBuilder, property)
    }
}

class GroovyWorkflowDefinition(val template: EnvironmentTemplate, val workflow : EnvWorkflow,
                               conversionService : ConversionService,
                               stepBuilderFactories : Seq[StepBuilderFactory],
                               override val repository: ProjectPropertyRepository,
                               override val projectId: Int) extends WorkflowDefinition with Logging with ProjectContextFromProperties {
    def convertAndValidate(value: Any, variable: VariableDetails): Seq[ValidationError] = {
      try {
        //all values stored as strings, so we need to use string repr. to convert here as well
        val typedVal = convert(String.valueOf(value), variable)

        (for (validator <- variable.validators) yield {
          if (!validator.call(typedVal))
            Some(ValidationError(variable.name, "Validation failed"))
          else
            None
        }).flatten.toSeq
      } catch {
        case e: ConversionException => Seq(ValidationError(e.fieldId, e.message))
      }
    }

    override def partial(variables: Map[String, Any]): Seq[VariableDescription] = {
        val dependents = workflow.variables.filter(p => p.dependsOn.sorted == variables.keys.toList.sorted).
          groupBy(_.name).map(_._2.head).toList

        val typedVars = variables.map(v => (v._1, convert(String.valueOf(v._2), workflow.variables.find(_.name == v._1)
            .getOrElse(throw new RuntimeException("No such variable: " + v._1)))))
        dependents.map(v => new VariableDescription(v.name, v.description, v.isOptional, null, v.valuesList.map(lambda => {
            lambda.apply(typedVars)
        }).getOrElse(Map()), if (v.dependsOn.isEmpty) None else Some(v.dependsOn.toList))).toSeq
    }


    def validate(variables: Map[String, Any], envName: Option[String] = None) = {
        val res = for (variable <- workflow.variables) yield {
            variables.get(variable.name) match {
                case None => {
                  variable.defaultValue match {
                    case Some(s) => convertAndValidate(String.valueOf(s), variable)
                    case None =>  if (variable.isOptional)
                        Seq()
                      else
                        Seq(ValidationError(variable.name, "This field is required"))
                  }
                }
                case Some(value) => {
                    convertAndValidate(value, variable)
                }
            }
        }

        res.flatten.toSeq
    }

    def convert(value: String, variable: VariableDetails): AnyRef = {
      try {
        conversionService.convert(value, variable.clazz)
      } catch {
        case _ => {
            val className = variable.clazz.getName
            throw new ConversionException(variable.name, "Conversion failed. Expected type is %s".format(className.substring(className.lastIndexOf('.') + 1)))
        }
      }
    }

    def embody(variables: Map[String, String], envName: Option[String] = None) = {
        val typedVariables = (for (variable <- workflow.variables) yield {
          val res = variables.get(variable.name) match {
            case Some(value) =>
              convert(value, variable)
            case None => {
              variable.defaultValue match {
                case Some(s) => convert(String.valueOf(s), variable)
                case _ => if (variable.isOptional)
                    null
                  else
                    throw new IllegalArgumentException("Variable '%s' is not defined".format(variable.name))
              }
            }
          }
          (variable.name, res)
        }).toMap
        val delegate = new StepBodiesCollector(typedVariables, stepBuilderFactories, repository, projectId)
        workflow.stepsGenerator match {
            case Some(generator) => {
                generator.setDelegate(delegate)
                generator.call()
                delegate.buildSteps.toSeq
            }
            case None => Seq()
        }
    }

    val variableDescriptions = {
        for (variable <- workflow.variables) yield {
            new VariableDescription(variable.name, variable.description, variable.isOptional, variable.defaultValue match {
              case None => null
              case Some(v) => String.valueOf(v)
            }, variable.valuesList.map(_.apply(Map())).getOrElse(Map()), if (variable.dependsOn.isEmpty) None else Some(variable.dependsOn.toList))
        }
    }

    val name = workflow.name
}

class GroovyTemplateDefinition(val envTemplate : EnvironmentTemplate,
                               conversionService : ConversionService,
                               stepBuilderFactories : Seq[StepBuilderFactory],
                               projectPropertiesRepository: ProjectPropertyRepository, projectId: Int) extends TemplateDefinition {
    def getWorkflow(name: String) = {
        envTemplate.workflows.filter(_.name==name).headOption.map(workflowDefinition(_))
    }

    def listWorkflows = {
        for (wf <- envTemplate.workflows) yield workflowDefinition(wf)
    }

    def destroyWorkflow = {
         workflowDefinition(envTemplate.workflows.filter(_.name==envTemplate.destroyWorkflow).head)
    }

    def createWorkflow = {
        workflowDefinition(envTemplate.workflows.filter(_.name==envTemplate.createWorkflow).head)
    }

    def workflowDefinition(workflow : EnvWorkflow) =
        new GroovyWorkflowDefinition(envTemplate, workflow, conversionService, stepBuilderFactories, projectPropertiesRepository, projectId)
}
