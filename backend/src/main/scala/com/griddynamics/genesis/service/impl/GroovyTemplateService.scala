/*
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
import com.griddynamics.genesis.util.{TryingUtil, ScalaUtils, Logging}
import org.codehaus.groovy.runtime.{InvokerHelper, MethodClosure}
import scala.Some
import service.ValidationError
import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.cache.Cache
import groovy.util.Expando
import net.sf.ehcache.{CacheManager, Element}
import java.util.concurrent.TimeUnit

class GroovyTemplateService(val templateRepository : TemplateRepository,
                            val stepBuilderFactories : Seq[StepBuilderFactory],
                            val conversionService : ConversionService,
                            val dataSourceFactories : Seq[DataSourceFactory] = Seq(),
                            databagRepository: DatabagRepository,
                            val cacheManager: CacheManager)
    extends service.TemplateService with Logging with Cache {

    val cache = addCacheIfAbsent("GroovyTemplateService")


    override def defaultTtl = TimeUnit.HOURS.toSeconds(24).toInt

    def findTemplate(projectId: Int, name: String, version: String) = getTemplate(projectId, name, version)

    def getTemplate(projectId: Int, name: String, version: String, eval: Boolean = true) : Option[TemplateDefinition] = {
        val body = getTemplateBody(name, version, projectId)
        body.flatMap(evaluateTemplate(projectId, _, None, None, None, listOnly = !eval).map(et =>
            new GroovyTemplateDefinition(et, conversionService, stepBuilderFactories)
        ))
    }

    override def descTemplate(projectId: Int, name: String, version: String) = {
      for (
        body <- getTemplateBody(name, version, projectId);
        definition <- evaluateTemplate(projectId, body, None, None, None, listOnly = true)
      ) yield {
        new TemplateDescription(name, version, definition.createWorkflow, definition.destroyWorkflow, definition.workflows.map(_.name))
      }
    }


  def getTemplateBody(name: String, version: String, projectId: Int): Option[String] = {
    val ref = Option(cache.get(TmplCacheKey(name, version, projectId))).map(_.getObjectValue.asInstanceOf[VersionedTemplate])
    val bodyOpt = ref match {
      case Some(verTmpl) => templateRepository.getContent(verTmpl)
      case None => templatesMap(projectId).get(name, version)
    }
    bodyOpt
  }

  def listTemplates(projectId: Int) = templatesMap(projectId).keys.toSeq

    private def templatesMap(projectId: Int) = {
        val sources = templateRepository.listSources()
        (for ((version, body) <- sources) yield try {
            val template = evaluateTemplate(projectId, body, None, None, None, true)

            template.foreach(t => {
              val key = TmplCacheKey(t.name, t.version, projectId)
              val value = new VersionedTemplate(version.name)
              cache.putIfAbsent(new Element(key, value))//todo (RB): we assume repo is not using vsc versions
            })

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

        val templateBuilder = if (listOnly) new NameVersionDelegate else new EnvTemplateBuilder(projectId, dataSourceFactories, databagRepository)
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
                          stepBuilderFactories : Seq[StepBuilderFactory])
    extends GroovyObjectSupport {

    val closures = (for (factory <- stepBuilderFactories) yield {
        (factory.stepName, (ListBuffer[Closure[Unit]](), factory))
    }).toMap

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
//            ScalaUtils.setProperty(stepBuilder, propertyName, contextAccess(context))
          InvokerHelper.setProperty(stepBuilder, propertyName, contextAccess(context))
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
          case null => {
            ScalaUtils.setProperty(stepBuilder, property, null)
            TryingUtil.silently(ScalaUtils.setProperty(this, property, null))
          }
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
                               stepBuilderFactories : Seq[StepBuilderFactory]) extends WorkflowDefinition with Logging {
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
        val appliedVars = variables.keys.toSet

        val dependents = workflow.variables.filter(p => p.dependsOn.toSet.subsetOf(appliedVars))

        val resolvedVariables = variables.map { case (varName, varValue) =>
          val variableDetails = workflow.variables.find(_.name == varName).getOrElse(throw new RuntimeException("No such variable: " + varName))
          val convertedValue = convert(String.valueOf(varValue), variableDetails)
          (varName, convertedValue)
        }

        for(v <- dependents) yield {
          val varPossibleValues: Map[String, String] = v.valuesList.map { lambda => lambda.apply(resolvedVariables) }.getOrElse(Map())
          val dependsOn = if (v.dependsOn.isEmpty) None else Some(v.dependsOn.toList)

          new VariableDescription(v.name, v.description, v.isOptional, v.defaultValue.map(_.toString).getOrElse(null), varPossibleValues, dependsOn)
        }
    }


    def validate(variables: Map[String, Any], envId: Option[Int] = None, projectId: Option[Int] = None) = {
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

    def embody(variables: Map[String, String], envId: Option[Int] = None, projectId: Option[Int] = None) = {
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
            val default  = variable.defaultValue.map(String.valueOf(_)).getOrElse(null)
            val dependsOn = if (variable.dependsOn.isEmpty) None else Some(variable.dependsOn.toList)

            val valueList: Map[String, String] = variable.valuesList.map(_.apply(Map())).getOrElse(Map())

            new VariableDescription(variable.name, variable.description, variable.isOptional, default, valueList, dependsOn)
        }
    }

    val name = workflow.name
}

private case class TmplCacheKey(name: String, version: String, projectId: Int)


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
