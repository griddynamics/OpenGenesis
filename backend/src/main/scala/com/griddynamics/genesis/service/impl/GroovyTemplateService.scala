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
import collection.mutable.ListBuffer
import com.griddynamics.genesis.api.{ExtendedResult, Configuration, Failure, Success}
import com.griddynamics.genesis.cache.{CacheConfig, CacheManager, Cache}
import com.griddynamics.genesis.model.{Environment, EntityAttr, EntityWithAttrs}
import com.griddynamics.genesis.plugin.GenesisStep
import com.griddynamics.genesis.plugin.{StepBuilder, StepBuilderFactory}
import com.griddynamics.genesis.repository.DatabagRepository
import com.griddynamics.genesis.service
import com.griddynamics.genesis.service._
import com.griddynamics.genesis.template._
import com.griddynamics.genesis.template.dsl.groovy._
import com.griddynamics.genesis.template.dsl.groovy.{Delegate => DslDelegate}
import com.griddynamics.genesis.util.{TryingUtil, ScalaUtils, Logging}
import com.griddynamics.genesis.workflow.Step
import groovy.lang._
import groovy.util.Expando
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit
import org.codehaus.groovy.runtime.{InvokerHelper, MethodClosure}
import org.springframework.core.convert.ConversionService
import com.griddynamics.genesis.annotation.RemoteGateway
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import support.VariablesSupport
import transformations.Context

@RemoteGateway("groovy template service")
class GroovyTemplateService(val templateRepoService : TemplateRepoService,
                            val stepBuilderFactories : Seq[StepBuilderFactory],
                            val conversionService : ConversionService,
                            val dataSourceFactories : Seq[DataSourceFactory] = Seq(),
                            databagRepository: DatabagRepository,
                            envConfigService: EnvironmentConfigurationService,
                            val cacheManager: CacheManager) extends service.TemplateService with Logging with Cache {

  val CACHE_NAME = "GroovyTemplateService"

  private def templateRepo(projectId: Int) = templateRepoService.get(projectId)

  override def defaultTtl = TimeUnit.HOURS.toSeconds(24).toInt

  def findTemplate(projectId: Int, templateName: String, templateVersion: String, envConfId: Int): Option[TemplateDefinition] = {
    val config = envConfigService.get(projectId, envConfId).getOrElse (
      throw new IllegalArgumentException (s"Couldn't find configuration $envConfId in projectId $projectId ")
    )
    getTemplate(projectId, templateName, templateVersion, envConf = config)
  }

  def findTemplate(env: Environment) =
    findTemplate(env.projectId, env.templateName, env.templateVersion, env.configurationId)

  def getTemplate(projectId: Int, name: String, version: String, envConf: Configuration) : Option[TemplateDefinition] = {
    val body = templateRawContent(projectId, name, version)
    body.flatMap(fullDefinition(_, projectId, envConf).map(et =>
      new GroovyTemplateDefinition(et, conversionService, stepBuilderFactories)
    ))
  }

  override def descTemplate(projectId: Int, name: String, version: String) = {
    for (
      body <- templateRawContent(projectId, name, version);
      definition <- highLevelDefinition(body)
    ) yield {
      new TemplateDescription(name, version, definition.createWorkflow, definition.destroyWorkflow, definition.workflows.map(_.name), body)
    }
  }

  def templateRawContent(projectId: Int, name: String, version: String): Option[String] = {
    val key = TmplCacheKey(name, version, projectId)
    val ref = fromCache(CACHE_NAME, key) { null.asInstanceOf[VersionedTemplate] }
    Option(ref) match {
      case Some(verTmpl) => templateRepo(projectId).getContent(verTmpl)
      case None => templatesMap(projectId).get(name, version).map(_.rawBody)
    }
  }

  def listTemplates(projectId: Int) = templatesMap(projectId).values.toSeq

  private def templatesMap(projectId: Int) = {
    def putInCache(template: EnvironmentTemplate, version: VersionedTemplate) {
      val key = TmplCacheKey(template.name, template.version, projectId)
      val value = new VersionedTemplate(version.name)
      cacheManager.createCacheIfAbsent(CacheConfig(CACHE_NAME, defaultTtl, maxEntries))
      cacheManager.putInCache(CACHE_NAME, key, value) //todo (RB): we assume repo is not using vsc versions
    }

    def templateDescription(template: EnvironmentTemplate, body: String): TemplateDescription = {
      new TemplateDescription(template.name, template.version, template.createWorkflow, template.destroyWorkflow, template.workflows.map(_.name), body)
    }

    val sources = templateRepo(projectId).listSources()

    (for ((version, body) <- sources) yield try {
        for(template <- highLevelDefinition(body)) yield {
          putInCache(template, version)
          ((template.name, template.version), templateDescription(template, body))
        }
      } catch {
        case t: Exception => {
            log.error(t, "Error in template name or version: %s", t)
            None
        }
    }).flatten.toMap
  }

  private def highLevelDefinition(body: String): Option[EnvironmentTemplate] = {
    buildTemplate(body, new NameVersionDelegate, None)
  }

  private def fullDefinition(body: String, projectId: Int, envConf: Configuration): Option[EnvironmentTemplate] = {
    val builder = new EnvTemplateBuilder(projectId, dataSourceFactories, databagRepository, envConf)
    buildTemplate(body, builder, Some(projectId))
  }

  private def buildTemplate(body: String, builder: TemplateBuilder with DslDelegate with GroovyObjectSupport, projectId: Option[Int]): Option[EnvironmentTemplate] = {
    val templateDecl = new BlockDeclaration
    val methodClosure = new MethodClosure(templateDecl, "declare")

    val binding = new Binding
    binding.setVariable("template", methodClosure)
    binding.setVariable("include", new MethodClosure(templateDecl, "include"))

    try {
      val compilerConfiguration = new CompilerConfiguration()
      compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(classOf[Context]))
      val groovyShell = new GroovyShell(binding, compilerConfiguration)
      groovyShell.evaluate(body)
      projectId.foreach (evaluateIncludes(_, templateDecl.includes, groovyShell))
    } catch {
      case e: GroovyRuntimeException => throw new IllegalStateException("can't process template", e)
    }
    templateDecl.bodies.headOption.map { body => DslDelegate(body).to(builder).newTemplate() }
  }

  private def getBody(projectId: Int, name: String) = templateRepo(projectId).
    listSources().
    collectFirst { case (nameVersion, body) if nameVersion.name.toUpperCase.endsWith(name.toUpperCase) => body }

  private def evaluateIncludes(projectId: Int, includes: Seq[String], groovyShell: GroovyShell) {
    for(include <- includes;
          body  <- getBody(projectId, include)) groovyShell.evaluate(body)
  }

  def clearCache(projectId: Int) {
    if (cacheManager.cacheExists(CACHE_NAME))
      cacheManager.removeFromCache(CACHE_NAME, {
        case TmplCacheKey(_, _, pId) => pId == projectId
        case _ => false
      })
  }
}

class StepBodiesCollector(val variables: Map[String, AnyRef],
                          stepBuilderFactories : Seq[StepBuilderFactory])
    extends GroovyObjectSupport with DslDelegate with VariablesSupport {

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

    def buildSteps = {
        (for ((bodies, factory) <- closures.values) yield {
            bodies.map { body => DslDelegate(body).to(new StepBuilderProxy(factory.newStepBuilder)) }
        }).flatten
    }
}

trait ContextAccess extends ((scala.collection.Map[String, Any]) => Any)

object UninitializedStepDetails extends Step {
  override def stepDescription = "..."
}

class GroovyAttrEntityWrapper(entity: EntityWithAttrs) extends GroovyObjectSupport {
  override def getProperty(property: String) = try {
    entity.get(EntityAttr(property)).getOrElse(InvokerHelper.getProperty(entity, property))
  } catch {
    case e: MissingPropertyException => null
  }
}

class StepBuilderProxy(stepBuilder: StepBuilder) extends GroovyObjectSupport with StepBuilder with DslDelegate with Logging {

  override def delegationStrategy = Closure.DELEGATE_FIRST

  private val contextDependentProperties = mutable.Map[String, ContextAccess]()
    def getDetails = if(contextDependentProperties.isEmpty) stepBuilder.getDetails else UninitializedStepDetails

    def newStep(context: scala.collection.Map[String, AnyRef], exports: (String, Any)*): GenesisStep = {
      val wrappedExports = exports.map {
        case (name, ent : EntityWithAttrs) => (name, new GroovyAttrEntityWrapper(ent))
        case x => x
      }
        contextDependentProperties.foreach { case (propertyName, contextAccess) =>
          InvokerHelper.setProperty(stepBuilder, propertyName, contextAccess(context ++ wrappedExports))
        }
        stepBuilder.id = this.id
        stepBuilder.phase = this.phase
        stepBuilder.exportTo = this.exportTo
        stepBuilder.ignoreFail = this.ignoreFail
        stepBuilder.precedingPhases = this.precedingPhases
        stepBuilder.title = this.title
        stepBuilder.skip = this.skip
        stepBuilder.newStep
    }

    override def newStep = newStep(Map())


    override def setProperty(property: String, value: Any) {
        log.trace(s"Deferred step building: setting ${stepBuilder.getClass}.$property = $value")
        (property, value) match {
            case ("skip", v: Closure[_]) => throw new RuntimeException("Skip cannot be a deferred object. Don't use syntax skip = {...}")
            case (_, value: Closure[_]) =>
                contextDependentProperties(property) = new ContextAccess {
                    def apply(v1: collection.Map[String, Any]) = {
                      import scala.collection.JavaConversions._
                      value.setDelegate(new Expando(v1){
                        override def getProperty(name: String) : AnyRef = {
                           if (name == Reserved.contextRef) {
                             this
                           } else {
                             super.getProperty(name)
                           }
                        }
                      })
                      value.call()
                    }
                }
            case (_, value: ContextAccess) =>
                contextDependentProperties(property) = value
            case (_, null) => {
                InvokerHelper.setProperty(stepBuilder, property, null)
                TryingUtil.silently(ScalaUtils.setProperty(this, property, null))
            }
            case (_, _) => {
                InvokerHelper.setProperty(stepBuilder, property, value)
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
    def convertAndValidate(value: Any, variable: VariableDetails, context: Map[String,Any]): Seq[ValidationError] = {
      def plainValue (value: Any) =  value match {
        case f: Option[_] => f.getOrElse(null)
        case a => a
      }

      try {
        //all values stored as strings, so we need to use string repr. to convert here as well
        val typedVal = convert(String.valueOf(value), variable)

        variable.validators.view.map { case (errorMsg, validator) =>
          validator.setDelegate(new Expando() with VariablesSupport {
           // must throw exception on missing properties(instead of return null) for delegation to work
            override def getProperty(name: String) = if (name != Reserved.varsRef) getProperties.containsKey(name) match {
              case true => super.getProperty(name)
              case _ => throw new MissingPropertyException(name, classOf[Expando])
            } else get$vars

            def variables = context.mapValues{plainValue(_)}
          })
          validator.setResolveStrategy(Closure.DELEGATE_FIRST)

          if (!validator.call(typedVal))
            Some(ValidationError(variable.name, errorMsg))
          else
            None
        }.find(_.isDefined).flatten.toSeq
      } catch {
        case e: ConversionException => Seq(ValidationError(e.fieldId, e.message))
      }
    }

    private def varDesc(v: VariableDetails, resolvedVariables: Map[String, Any] = Map()) = {
      val dependsOn = if (v.dependsOn.isEmpty) None else Some(v.dependsOn.toList)
      val (varDsDefault, possibleValues) = v.valuesList.map(_.apply(resolvedVariables)) match {
        case None => (v.defaultValue(), None)
        // if datasource defines some default value but no list, then it should be rendered as input(not as select)
        case Some((default@Some(_), values)) if values.isEmpty => (default, None)
        case Some((default, values)) => (default, Option(values))
      }

      new VariableDescription(v.name, v.clazz, v.description, v.isOptional, v.defaultValue().map(String.valueOf(_)).getOrElse(varDsDefault.map(String.valueOf(_)).getOrElse(null)),
        possibleValues, dependsOn, v.group.map(_.description))
    }

    override def partial(variables: Map[String, Any]): Seq[VariableDescription] = {
        val appliedVars = variables.keys.toSet

        val dependents = workflow.variables().filter(
          p =>
            p.dependsOn.toSet.subsetOf(appliedVars)
        )

        val resolvedVariables = variables.map { case (varName, varValue) =>
          val variableDetails = workflow.variables().find(_.name == varName).getOrElse(throw new RuntimeException("No such variable: " + varName))
          val convertedValue = convert(String.valueOf(varValue), variableDetails)
          (varName, convertedValue)
        }

        for(v <- dependents) yield varDesc(v, resolvedVariables)
    }

  def validatePreconditions(variables: Map[String, Any], config: Configuration): ExtendedResult[_] = {
    val errors = workflow.preconditions.map { case (validationFailureMessage, checkClosure) =>
//      checkClosure.setProperty(Reserved.configRef, EnvConfigSupport.asGroovyMap(config)) TODO: should it be removed completely?
      checkClosure.setDelegate(new ScopeHolder(variables))
      if (!checkClosure.call()) Some(validationFailureMessage) else None
    }.flatten.toSeq

    if (!errors.isEmpty)
      Failure(compoundServiceErrors = errors)
    else
      Success(None)
  }
  
  def validate(variables: Map[String, Any]) = {
    def isValueProvided(variable: VariableDetails) = variables.contains(variable.name)

    val varDetails = workflow.variables()

    val context = (for (variable <- varDetails) yield {
      (variable.name, variables.get(variable.name).map(v =>
        try {
          convert(String.valueOf(v), variable)
        } catch {
          case e: Throwable => null //todo: Some(null) ???
        }
      ))
    })

    val groupVars: Map[GroupDetails, List[VariableDetails]] = varDetails.groupBy(_.group).collect{case (Some(g), v) => (g,v)}

    val groupErrors = groupVars.map {
      case (group, vars) => vars.count(isValueProvided(_)) match {
        case 0 if group.required => Seq(ValidationError(vars.head.name, "%s is required: please select non-empty value".format(group.description)))
        case 1 => Seq()
        case _ => vars.collect { case v if isValueProvided(v)=>
          ValidationError(v.name, "No more than one variable in group '%s' could have value".format(group.description))
        }
      }
    }.flatten

    val variablesErrors = (for (variable <- varDetails) yield {
      variables.get(variable.name) match {
        case None =>
          variable.group.map { groupVars(_).map(_.name).intersect(variables.keys.toSeq) } match {
            // if some other variable from the same group has value, then don't validate this default
            case Some(x) if x.nonEmpty => Seq()

            case _ => variable.defaultValue() match {
              case Some(s) => convertAndValidate(String.valueOf(s), variable, context.toMap) //validate default
              case None if variable.isOptional => Seq()
              case None => Seq(ValidationError(variable.name, "This field is required"))
            }
          }
        case Some(value) => convertAndValidate(value, variable, context.toMap)
      }
    }).flatten

    (groupErrors ++ variablesErrors).toSeq
  }

    def convert(value: String, variable: VariableDetails): AnyRef = {
      try {
        conversionService.convert(value, variable.clazz)
      } catch {
        case _: Throwable => {
            val className = variable.clazz.getName
            throw new ConversionException(variable.name, "Conversion failed. Expected type is %s".format(className.substring(className.lastIndexOf('.') + 1)))
        }
      }
    }

    def embody(variables: Map[String, String], envId: Option[Int] = None, projectId: Option[Int] = None) = {
      val varDetails = workflow.variables()
      val groupVars = varDetails.groupBy(_.group).collect{case (Some(g), v) => (g,v)}
        val typedVariables = (for (variable <- varDetails) yield {
          val res = variables.get(variable.name) match {
            case Some(value) =>
              convert(value, variable)
            case None => variable.group.map(groupVars(_).map(_.name).intersect(variables.keys.toSeq)) match {
                // if some other variable from the same group has value, then don't use default
                case Some(x) if x.nonEmpty => null
                case _ => variable.defaultValue() match {
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
        val regularSteps = createSteps (workflow.stepsGenerator, typedVariables)
        val rescueSteps = createSteps(workflow.rescues, typedVariables)
        Builders(regularSteps, rescueSteps)
    }

    private def createSteps(generator: Option[Closure[Unit]], variables: Map[String, AnyRef]): Seq[StepBuilderProxy] =
        generator.map { it =>
          DslDelegate(it).to(new StepBodiesCollector(variables.filterKeys(Reserved.configRef != _), stepBuilderFactories)).buildSteps.toSeq
        }.getOrElse(Seq())

    lazy val variableDescriptions = for (variable <- workflow.variables()) yield varDesc(variable)

    val name = workflow.name
}

private case class TmplCacheKey(name: String, version: String, projectId: Int)


class GroovyTemplateDefinition(val envTemplate : EnvironmentTemplate,
                               conversionService : ConversionService,
                               stepBuilderFactories : Seq[StepBuilderFactory]) extends TemplateDefinition {
    def getWorkflow(name: String) = {
        envTemplate.workflows.filter(_.name==name).headOption.map(workflowDefinition(_))
    }

    override def getValidWorkflow(name: String) =  {
        envTemplate.workflows.find(_.name == name).map(w =>
            {
                val errors = w.preconditions.map { case (errorMessage, checkClosure) =>
                  try {
                    if (! checkClosure.call())
                        Some(errorMessage)
                    else
                        None
                  } catch {
                    case e: MissingPropertyException => None
                  }
                }.flatten.toSeq
                if (! errors.isEmpty)
                    Failure(compoundServiceErrors = errors)
                else
                    Success(workflowDefinition(w))
            }
        ) match {
            case Some(s) => s
            case _ => Failure(isNotFound = true)
        }
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

    def workflowDefinition(workflow : EnvWorkflow) = {
        new GroovyWorkflowDefinition(envTemplate, workflow, conversionService, stepBuilderFactories)
    }
}
