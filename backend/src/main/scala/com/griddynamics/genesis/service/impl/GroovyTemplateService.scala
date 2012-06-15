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
import org.codehaus.groovy.runtime.MethodClosure
import groovy.lang._
import collection.mutable.ListBuffer
import service._
import org.springframework.core.convert.ConversionService
import java.lang.IllegalStateException
import scala.Some
import reflect.BeanProperty
import java.util.{Map => JMap}
import com.griddynamics.genesis.template.dsl.groovy._
import com.griddynamics.genesis.template.{DataSourceFactory, VersionedTemplate, TemplateRepository}
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.plugin.{GenesisStep, StepBuilder, StepBuilderFactory}
import com.griddynamics.genesis.util.{ScalaUtils, Logging}

class GroovyTemplateService(val templateRepository : TemplateRepository,
                            val stepBuilderFactories : Seq[StepBuilderFactory],
                            val conversionService : ConversionService,
                            val dataSourceFactories : Seq[DataSourceFactory] = Seq()   )
    extends service.TemplateService with Logging {

    val updateLock = new Object

    var revisionId = ""
    var sourcesMap = Map[VersionedTemplate, (String, String)]()
    var templatesMap = Map[(String, String), EnvironmentTemplate]()

    def findTemplate(projectId: Int, name: String, version: String) : Option[TemplateDefinition] =
        updateTemplates(projectId).get(name, version).map(et =>
            new GroovyTemplateDefinition(et, conversionService, stepBuilderFactories)
        )

    def listTemplates(projectId: Int) = {
        val sources = templateRepository.listSources()
        (for ((version, body) <- sources) yield try {
            val template = evaluateTemplate(projectId, body, None, None, None, true)
            template.map(t => (t.name, t.version))
        } catch {
            case t: Throwable => {
                log.error(t, "Error in template name or version: %s", t)
                None
            }
        }).flatten.toSeq

    }

    //TODO: remove?
    private def updateTemplates(projectId: Int) = updateLock.synchronized {
        val sources = templateRepository.listSources()
        val uRevisionId = TemplateRepository.revisionId(sources)

        if (revisionId != uRevisionId) {
            revisionId = uRevisionId

            val uSourcesMap = mutable.Map[VersionedTemplate, (String, String)]()
            val uTemplatesMap = mutable.Map[(String, String), EnvironmentTemplate]()

            for ((version, body) <- sources) try {
                val pair = sourcesMap.get(version)
                val template : Option[EnvironmentTemplate] = pair match {
                  case None => evaluateTemplate(projectId, body, None, None, None)
                  case Some(p) => templatesMap.get(p) match {
                      case a@Some(t) => a
                      case _ => evaluateTemplate(projectId, body, None, None, None)
                  }
                }
                template.foreach(template => {
                    uSourcesMap(version) = (template.name, template.version)
                    uTemplatesMap((template.name, template.version)) = template
                })
            }  catch {
                // TODO: log exception stack trace?
                case t: Throwable => {
                    log.error(t, "Error processing template: %s", t)
                    None
                }
            }
            sourcesMap = uSourcesMap.toMap
            templatesMap = uTemplatesMap.toMap
        }

        templatesMap
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

        val templateBuilder = if (listOnly) new NameVersionDelegate else new EnvTemplateBuilder(projectId, dataSourceFactories)
        templateDecl.bodies.headOption.map {
            templateBody => {
                templateBody.setDelegate(templateBuilder)
                templateBody.call()
                templateBuilder.newTemplate(extName, extVersion, extProject)
            }
        }
    }

    def templateRawContent(projectId: Int, name: String, version: String) = {
      updateTemplates(projectId)

      val templVersionOption = sourcesMap.find { case (_, nameVersionTuple) =>
        nameVersionTuple._1 == name && nameVersionTuple._2 == version
      }.map(_._1)

      templVersionOption.flatMap ( templateRepository.getContent(_))
    }
}

class StepBodiesCollector(variables: Map[String, AnyRef],
                          stepBuilderFactories : Seq[StepBuilderFactory])
    extends GroovyObjectSupport {

    val closures = (for (factory <- stepBuilderFactories) yield {
        (factory.stepName, (ListBuffer[Closure[Unit]](), factory))
    }).toMap


    def $(evalExpression: String) = {
     new ContextAccess {
       def apply(context: scala.collection.Map[String, Any]) = {

         val binding = new Binding()
         context.foreach { case (key, value) => binding.setVariable(key, value) }

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
                val stepBuilder = new StepBuilderProxy(factory.newStepBuilder)
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

class StepBuilderProxy(stepBuilder: StepBuilder) extends GroovyObjectSupport with StepBuilder {
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
          case value: ContextAccess =>
            contextDependentProperties(property) = value.asInstanceOf[ContextAccess]
          case _ => {
            ScalaUtils.setProperty(stepBuilder, property, value)
            if (ScalaUtils.hasProperty(this, property, ScalaUtils.getType(value))) {
              ScalaUtils.setProperty(this, property, value)
            }
          }
        }
    }
}

class GroovyWorkflowDefinition(val template: EnvironmentTemplate, val workflow : EnvWorkflow,
                               conversionService : ConversionService,
                               stepBuilderFactories : Seq[StepBuilderFactory]) extends WorkflowDefinition {
    def convertAndValidate(value: Any, variable: VariableDetails): Seq[ValidationError] = {
      try {
        //all values stored as strings, so we need to use string repr. to convert here as well
        val typedVal = conversionService.convert(String.valueOf(value), variable.clazz)

        (for (validator <- variable.validators) yield {
          if (!validator.call(typedVal))
            Some(ValidationError(variable.name, "Validation failed for variable '%s'".format(variable.name)))
          else
            None
        }).flatten.toSeq
      } catch {
        case e => val className: String = variable.clazz.getName
        Seq(ValidationError(variable.name, "Conversion failed for variable %s. Expected type is %s".format(variable.name, className.substring(className.lastIndexOf('.') + 1))))
      }
    }

    override def partial(variables: Map[String, Any]): Seq[VariableDescription] = {
        val dependents = for (variable <- variables) yield {
            workflow.variables.find(p => p.dependsOn.find(_ == variable._1).isDefined).get
        }
        dependents.map(v => new VariableDescription(v.name, v.description, v.isOptional, null, v.valuesList.map(lambda => {
            lambda.apply(variables)
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
                        Seq(ValidationError(variable.name, "Variable '%s' is not set".format(variable.name)))
                  }
                }
                case Some(value) => {
                    convertAndValidate(value, variable)
                }
            }
        }

        res.flatten.toSeq
    }

    def embody(variables: Map[String, String], envName: Option[String] = None) = {
        def convert(value: String, variable: VariableDetails): AnyRef = {
          try {
            conversionService.convert(value, variable.clazz)
          } catch {
            case _ => throw new IllegalArgumentException("Variable '%s' has an invalid format: %s".format(variable.name, String.valueOf(value)))
          }
        }
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
        val delegate = new StepBodiesCollector(typedVariables, stepBuilderFactories)
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
                               stepBuilderFactories : Seq[StepBuilderFactory]) extends TemplateDefinition {
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
        new GroovyWorkflowDefinition(envTemplate, workflow, conversionService, stepBuilderFactories)
}
