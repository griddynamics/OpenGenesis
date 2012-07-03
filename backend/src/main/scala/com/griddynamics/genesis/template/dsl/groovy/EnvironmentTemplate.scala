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
package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.{GroovyObjectSupport, Closure}
import scala._
import collection.mutable.ListBuffer
import com.griddynamics.genesis.template._
import java.lang.reflect.Method
import java.lang.{Boolean, IllegalStateException}
import com.griddynamics.genesis.util.ScalaUtils
import scala.Some
import com.griddynamics.genesis.repository.ProjectPropertyRepository

class EnvWorkflow(val name : String, val variables : List[VariableDetails], val stepsGenerator : Option[Closure[Unit]])

class EnvironmentTemplate(val name : String,
                          val version : String,
                          val projectId : Option[String],
                          val createWorkflow : String,
                          val destroyWorkflow : String,
                          val workflows : List[EnvWorkflow]) {
}

class VariableDetails(val name : String, val clazz : Class[_ <: AnyRef], val description : String,
                      val validators : Seq[Closure[Boolean]], val isOptional: Boolean = false, val defaultValue: Option[Any],
                      val valuesList: Option[(Map[String,Any] => Map[String,String])] = None, val dependsOn: Seq[String])

class VariableBuilder(val name : String, dsObjSupport: Option[DSObjectSupport]) {
    var description : String = _
    var validators = new ListBuffer[Closure[Boolean]]
    var clazz : Class[_ <: AnyRef] = classOf[String]
    var defaultValue: Any = _
    var isOptional: Boolean = false
    var parents = new ListBuffer[String]
    var dataSource: Option[String] = None
    var useOneOf: Boolean = false
    var oneOf: Closure[java.util.Map[String,String]] = _

    def as(value : Class[_ <: AnyRef]) = {
        this.clazz = value
        this
    }

    def description(description : String) = {
        description_=(description)
        this
    }

    def validator(validator : Closure[Boolean]) = {
        validators += validator
        this
    }
  
    def optional(v: Any) = {
      isOptional = true
      defaultValue = v
      this
    }

    def dependsOn(varName: String) = {
        if (useOneOf) {
            throw new IllegalArgumentException("dependsOn cannot be used with oneOf")
        }
        parents += varName
        this
    }

    def dependsOn(names: Array[String]) = {
        if (useOneOf) {
            throw new IllegalArgumentException("dependsOn cannot be used with oneOf")
        }
        parents ++= names
        this
    }

    def dataSource(dsName: String) = {
        if (useOneOf) {
            throw new IllegalArgumentException("oneOf cannot be used with dataSource")
        }
        dataSource_=(Option(dsName))
        this
    }

    def oneOf(values: Closure[java.util.Map[String,String]]) = {
        useOneOf = true
        oneOf_=(values)
        this
    }

    def valuesList: Option[(Map[String, Any] => Map[String,String])] = {
        if (useOneOf) {
            import collection.JavaConversions._
            dsObjSupport.foreach(oneOf.setDelegate(_))
            val values = Option({ _: Any => oneOf.call().map(kv => (kv._1, kv._2)).toMap})
            validators += new Closure[Boolean]() {
                def doCall(args: Array[Any]): Boolean = {
                    values.get.apply().asInstanceOf[Map[String,String]].exists(_._1.toString == args(0).toString)
                }
            }
            values
        } else {
           dataSource.flatMap(ds => Option({params : Map[String, Any] => {
               val p = parents.toList.map(params.get(_)).flatten
               dsObjSupport.get.getData(ds, p)
           }}))
        }
    }

    def newVariable = new VariableDetails(name, clazz, description, validators, isOptional, Option(defaultValue), valuesList, parents.toList)
}

class VariableDeclaration(val dsObjSupport: Option[DSObjectSupport]) {
    val builders = new ListBuffer[VariableBuilder]

    def variable(name : String) = {
        val builder = new VariableBuilder(name, dsObjSupport)
        builders += builder
        builder
    }
}

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

class NameVersionDelegate {
    var name : String = _
    var version : String = _
    def workflow(name: String, details : Closure[Unit]) = this

    def name(value : String) : NameVersionDelegate = {
        if (name != null)
            throw new IllegalStateException("name is already set")
        this.name = value
        this
    }

    def version(value : String): NameVersionDelegate = {
        if (version != null) throw new IllegalStateException("version is already set")
        this.version = value
        this
    }

    def newTemplate(extName: Option[String], extVersion: Option[String], extProject: Option[String]) = {
      val templateName = extName match {
        case None => name
        case Some(s) => s
      }
      val templateVersion = extVersion match {
        case None => version
        case Some(s) => s
      }
      new EnvironmentTemplate(templateName, templateVersion, extProject, "create", "destroy",
          List(new EnvWorkflow("create", List(), None), new EnvWorkflow("destroy", List(), None)))
    }

    def createWorkflow(name : String) = this    
    def destroyWorkflow(name : String)  = this
    def dataSources(ds : Closure[Unit]){}
}

class EnvTemplateBuilder(val projectId: Int, val dataSourceFactories : Seq[DataSourceFactory], ppRepository: ProjectPropertyRepository) extends NameVersionDelegate {

    var createWorkflow : String = _
    var destroyWorkflow : String = _

    val workflows = ListBuffer[EnvWorkflow]()
    
    var dsObjSupport : Option[DSObjectSupport] = None

    override def workflow(name: String, details : Closure[Unit]) = {
        if (workflows.find(_.name == name).isDefined)
            throw new IllegalStateException("workflow with name '%s' is already defined".format(name))
        val delegate = new WorkflowDeclaration
        details.setDelegate(delegate)
        details.call()
        val pid = projectId
        val variableBuilders = delegate.variablesBlock match {
            case Some(block) => {
                val variablesDelegate = new VariableDeclaration(dsObjSupport) with ProjectContextFromProperties {
                  override val repository = ppRepository
                  override val projectId = pid
                }
                block.setDelegate(variablesDelegate)
                block.call()
                variablesDelegate.builders
            } case None => Seq[VariableBuilder]()
        }

        val variables = for(builder <- variableBuilders) yield builder.newVariable

        workflows += new EnvWorkflow(name, variables.toList, delegate.stepsBlock)
        this
    }

    override def createWorkflow(name : String) : EnvTemplateBuilder = {
        if (createWorkflow != null) throw new IllegalStateException("create workflow name is already set")
        this.createWorkflow = name
        this
    }

    override def destroyWorkflow(name : String) : EnvTemplateBuilder = {
        if (destroyWorkflow != null) throw new IllegalStateException("destroy workflow name is already set")
        this.destroyWorkflow = name
        this
    }

    override def dataSources(ds : Closure[Unit]) {
        val pid = projectId
        val dsDelegate = new DataSourceDeclaration(projectId, dataSourceFactories) with ProjectContextFromProperties {
          override val repository = ppRepository
          override val projectId = pid
        }
        ds.setDelegate(dsDelegate)
        ds.call()
        val dsBuilders = dsDelegate.builders
        val map = (for (builder <- dsBuilders) yield builder.newDS).toMap
        dsObjSupport = Option(new DSObjectSupport(map))
    }

    private def newTemplate = {
        if (name == null) throw new IllegalStateException("name is not set")
        if (version == null) throw new IllegalStateException("version is not set")
        if (createWorkflow == null) throw new IllegalStateException("create workflow name is not set")
        if (destroyWorkflow == null) throw new IllegalStateException("destroy workflow name is not set")
        if (workflows.find(_.name == createWorkflow).isEmpty) throw new IllegalStateException("create workflow is not defined")
        if (workflows.find(_.name == destroyWorkflow).isEmpty) throw new IllegalStateException("destroy workflow is not defined")
        new EnvironmentTemplate(name, version, None, createWorkflow, destroyWorkflow, workflows.toList)
    }
  
    override def newTemplate(extName: Option[String], extVersion: Option[String], extProject: Option[String]) = {
      val templateName = extName match {
        case None => name
        case Some(s) => s
      }
      val templateVersion = extVersion match {
        case None => version
        case Some(s) => s
      }
      if (templateName == null) throw new IllegalStateException("name is not set")
      if (templateVersion == null) throw new IllegalStateException("version is not set")
      if (createWorkflow == null) throw new IllegalStateException("create workflow name is not set")
      if (destroyWorkflow == null) throw new IllegalStateException("destroy workflow name is not set")
      if (workflows.find(_.name == createWorkflow).isEmpty) throw new IllegalStateException("create workflow is not defined")
      if (workflows.find(_.name == destroyWorkflow).isEmpty) throw new IllegalStateException("destroy workflow is not defined")
      new EnvironmentTemplate(templateName, templateVersion, extProject, createWorkflow, destroyWorkflow, workflows.toList)
    }
}

class BlockDeclaration {
    val bodies = ListBuffer[Closure[Unit]]()

    def declare(body : Closure[Unit]) {
        if (body == null) return
        bodies += body
    }
}

class DataSourceDeclaration(val projectId: Int, dsFactories: Seq[DataSourceFactory]) extends GroovyObjectSupport {
    val builders = new ListBuffer[DataSourceBuilder]

    override def invokeMethod(name: String, args: AnyRef) = {
        dsFactories.filter(ds => ds.mode == name).headOption match {
            case Some(factory) => {
                val argsIterator: Iterator[_] = args.asInstanceOf[Array[_]].iterator
                if (argsIterator.isEmpty) {
                    throw new IllegalArgumentException("Both name and configuration must be provided for datasource %s".format(name))
                }
                val dsName = argsIterator.next().asInstanceOf[String]
                val builder: DataSourceBuilder = new DataSourceBuilder(projectId, factory, dsName)
                if (argsIterator.hasNext) {
                    val closure = argsIterator.next().asInstanceOf[Closure[_]]
                    closure.setDelegate(builder)
                    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
                    closure.call()
                }
                builders += builder
            }
            case _ => throw new IllegalArgumentException("Datasource for mode %s is not found".format(name))
        }
    }
}

class DataSourceBuilder(val projectId: Int, val factory : DataSourceFactory, val name: String) extends GroovyObjectSupport {
    var conf = new scala.collection.mutable.HashMap[String, Any]()

    override def setProperty(name: String, args: AnyRef) {
        conf.put(name, args)
        super.setProperty(name, args)
    }

    def newDS = (name, {val ds = factory.newDataSource; ds.config(conf.toMap + ("projectId" -> projectId)); ds})
}

 class DSObjectSupport(val dsMap: Map[String, VarDataSource]) extends GroovyObjectSupport {
     override def getProperty(name: String)  = {
         dsMap.get(name) match {
             case Some(src) => collection.JavaConversions.mapAsJavaMap(src.getData)
             case _ => super.getProperty(name)
         }
     }

     def getData(name: String, args: List[Any]): Map[String,String] = {
         dsMap.get(name) match {
             case Some(src) => args match {
                 case Nil => src.getData
                 case x :: Nil => {
                     src.asInstanceOf[DependentDataSource].getData(x)
                 }
                 case head :: tail => {
                     val params: Array[AnyRef] = args.map(v => ScalaUtils.toAnyRef(v)).toArray
                     val find: Option[Method] = src.getClass.getDeclaredMethods.find(m => m.getName == "getData"
                       && m.getParameterTypes.length == params.length
                     )
                     find match  {
                         case Some(m) => {
                             m.invoke(src, params:_*).asInstanceOf[Map[String,String]]
                         }
                         case _ => throw new IllegalStateException("Cannot find method getData for args %s".format(args))
                     }
                 }
                 case _ => throw new IllegalStateException("Cannot find any suitable method at datasource %s".format(src))
             }
             case _ => throw new IllegalStateException("Can't get datasource for argument %s".format(name))
         }
     }
 }
