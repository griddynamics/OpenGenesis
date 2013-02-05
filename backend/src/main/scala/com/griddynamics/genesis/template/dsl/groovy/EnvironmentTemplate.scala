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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.template.dsl.groovy

import groovy.lang.{Binding, GroovyObjectSupport, Closure}
import scala._
import collection.mutable.ListBuffer
import com.griddynamics.genesis.template._
import java.lang.IllegalStateException
import scala.Some
import com.griddynamics.genesis.repository.DatabagRepository
import support.{EnvConfigSupport, UnifiedDatabagSupport, ProjectDatabagSupport, SystemWideContextSupport}
import com.griddynamics.genesis.api.{Success, Configuration}
import com.griddynamics.genesis.service.EnvironmentService
import org.apache.commons.lang3.math.NumberUtils

class EnvironmentTemplate(val name : String,
                          val version : String,
                          val projectId : Option[String],
                          val createWorkflow : String,
                          val destroyWorkflow : String,
                          val workflows : List[EnvWorkflow]) {
}

class NameVersionDelegate extends GroovyObjectSupport with Delegate {
    var name : String = _
    var version : String = _
    var createWorkflowName : String = _
    var destroyWorkflowName : String = _
    val workflows = ListBuffer[EnvWorkflow]()

    def workflow(name: String, details : Closure[Unit]) = {
        workflows += new EnvWorkflow(name, None)
        this
    }

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
      val templateName = extName.getOrElse(name)
      val templateVersion = extVersion.getOrElse(version)
      new EnvironmentTemplate(templateName, templateVersion, extProject, createWorkflowName, destroyWorkflowName,
          workflows.toList)
    }

    def createWorkflow(name : String): NameVersionDelegate = {
        this.createWorkflowName = name
        this
    }

    def destroyWorkflow(name : String): NameVersionDelegate = {
        this.destroyWorkflowName = name
        this
    }
    def dataSources(ds : Closure[Unit]){}

  // STOP delegating to owner here: owner is just groovy script
  override def delegationStrategy = Closure.DELEGATE_ONLY
}

class EnvTemplateBuilder(val projectId: Int,
                         val dataSourceFactories : Seq[DataSourceFactory],
                         val databagRepository: DatabagRepository,
                         envConfigService: EnvironmentService,
                         envConfigOpt: Option[Configuration],
                         binding: Binding) extends NameVersionDelegate with SystemWideContextSupport
                         with ProjectDatabagSupport with UnifiedDatabagSupport {

    var dsObjSupport : Option[DSObjectSupport] = None
    var dsClozures: Option[Closure[Unit]] = None
  private var envConfig: AnyRef = _

    override def workflow(name: String, details : Closure[Unit]) = {
        if (workflows.find(_.name == name).isDefined)
            throw new IllegalStateException("workflow with name '%s' is already defined".format(name))

        val delegate = Delegate(details)
          .to(new WorkflowDeclaration(dsClozures, dataSourceFactories, projectId, defaultEnvConfigId(name)))

        workflows += new EnvWorkflow(name, delegate.stepsBlock,
            preconditions = delegate.requirements.toMap, rescues = delegate.rescueBlock) {
            override def variables() = delegate.variables
        }
        this
    }

  private def defaultEnvConfigId(name: String) = if (createWorkflowName == name)
    envConfigService.getDefault(projectId) match {
      case Some(c) => c.id
      case _ => None
    }
  else None

    override def createWorkflow(name : String) : EnvTemplateBuilder = {
        if (createWorkflowName != null) throw new IllegalStateException("create workflow name is already set")
        this.createWorkflowName = name
        this
    }

    override def destroyWorkflow(name : String) : EnvTemplateBuilder = {
        if (destroyWorkflowName != null) throw new IllegalStateException("destroy workflow name is already set")
        this.destroyWorkflowName = name
        this
    }


    override def dataSources(ds : Closure[Unit]) {
        val dsDelegate = new DataSourceDeclaration(projectId, dataSourceFactories)
        ds.setDelegate(dsDelegate)
        dsClozures = Some(ds)
    }

    private def newTemplate = {
        if (name == null) throw new IllegalStateException("name is not set")
        if (version == null) throw new IllegalStateException("version is not set")
        if (createWorkflowName == null) throw new IllegalStateException("create workflow name is not set")
        if (destroyWorkflowName == null) throw new IllegalStateException("destroy workflow name is not set")
        if (workflows.find(_.name == createWorkflowName).isEmpty) throw new IllegalStateException("create workflow is not defined")
        if (workflows.find(_.name == destroyWorkflowName).isEmpty) throw new IllegalStateException("destroy workflow is not defined")
        new EnvironmentTemplate(name, version, None, createWorkflowName, destroyWorkflowName, workflows.toList)
    }


  override def getProperty(name: String) = name match {
    case Reserved.configRef => envConfigOpt.map(EnvConfigSupport.asGroovyMap(_)).getOrElse(if (envConfig != null) envConfig else super.getProperty(name))
    case _ => if (binding.hasVariable(name)) binding.getProperty(name) else super.getProperty(name)
  }

  override def setProperty(property: String, newValue: Any) {
    (property, newValue) match {
      case (Reserved.configRef, s: String) if NumberUtils.isDigits(s) => envConfigService.get(projectId, s.toInt).foreach(
        config => envConfig = EnvConfigSupport.asGroovyMap(config))
      case (Reserved.configRef, conf: AnyRef) => envConfig = conf
      case _ => // DO NOTHING HERE
    }
  }

  override def invokeMethod(name: String, args: AnyRef) = {
    if (binding.hasVariable(name)) binding.getProperty(name) match {
      case cl: Closure[AnyRef] => cl.call(args)
      case _ => super.invokeMethod(name, args)
    }
    else super.invokeMethod(name, args)
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
      if (createWorkflowName == null) throw new IllegalStateException("create workflow name is not set")
      if (destroyWorkflowName == null) throw new IllegalStateException("destroy workflow name is not set")
      if (workflows.find(_.name == createWorkflowName).isEmpty) throw new IllegalStateException("create workflow is not defined")
      if (workflows.find(_.name == destroyWorkflowName).isEmpty) throw new IllegalStateException("destroy workflow is not defined")
      new EnvironmentTemplate(templateName, templateVersion, extProject, createWorkflowName, destroyWorkflowName, workflows.toList)
    }
}

class BlockDeclaration {
    val bodies = ListBuffer[Closure[Unit]]()
    val includes = ListBuffer[String]()

    def declare(body : Closure[Unit]) {
        if (body == null) return
        bodies += body
    }

    def include(path: String) {
        if (path == null) return
        includes += path
    }
}




