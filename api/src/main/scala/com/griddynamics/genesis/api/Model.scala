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
package com.griddynamics.genesis.api

import java.util
import com.griddynamics.genesis.validation.FieldConstraints._
import java.sql.Timestamp
import util.{Calendar, TimeZone, Locale}
import java.text.DateFormat

trait Identifiable[T] {
  def id: T
}

case class Environment(id: Int,
                       name : String,
                       status : String,
                       workflowCompleted : Option[Double] = None, //optional field, only if status Executing(workflow)
                       creator : String,
                       creationTime: Long,
                       modificationTime: Option[Long],
                       modifiedBy: Option[String],
                       templateName : String,
                       templateVersion : String,
                       projectId: Int,
                       attributes: Map[String, Attribute]) extends Identifiable[Int]

case class Attribute(value: String, description: String)

case class EnvironmentDetails(envId: Int,
                              name : String,
                              status : String,
                              creator : String,
                              creationTime: Long,
                              modificationTime: Option[Long],
                              modifiedBy: Option[String],
                              templateName : String,
                              templateVersion : String,
                              workflows : Seq[Workflow],
                              createWorkflowName : String,
                              destroyWorkflowName : String,
                              vms : Seq[VirtualMachine],
                              servers : Seq[BorrowedMachine],
                              projectId: Int,
                              historyCount: Int,
                              workflowCompleted: Option[Double],
                              attributes: Map[String, Attribute] = Map())

case class Variable(name : String, description : String, optional: Boolean = false, defaultValue: String = null,
                    values:Map[String,String] = Map(), dependsOn: Option[List[String]] = None)

case class Template(name : String, version : String, createWorkflow : Workflow, workflows : Seq[Workflow])

case class Workflow(name : String, variables : Seq[Variable])

case class WorkflowStep(stepId: String, phase: String, status : String, details : String, started: Option[Long],
                        finished: Option[Long], title: Option[String], regular: Boolean)

case class WorkflowDetails(name : String,
                           status: String,
                           startedBy: String,
                           stepsCompleted: Option[Double],
                           steps : Option[Seq[WorkflowStep]],
                           executionStartedTimestamp: Option[Long],
                           executionFinishedTimestamp: Option[Long])

case class WorkflowHistory(history: Option[Seq[WorkflowDetails]] = None,
                           totalCount: Int = 0)

case class VirtualMachine(envName : String,
                          roleName : String,
                          hostNumber : Int,
                          instanceId : String,
                          hardwareId : String,
                          imageId : String,
                          publicIp : String,
                          privateIp : String,
                          status : String)

case class BorrowedMachine(envName: String, roleName: String, instanceId: String, address: String, status: String)

/**
 * @deprecated use {@see com.griddynamics.genesis.api.ExtendedResult} instead
 */
@deprecated("Use ExtendedResult instead of RequestResult", "1.1.0")
case class RequestResult(serviceErrors : Map[String, String] = Map(),
                         variablesErrors : Map[String, String] = Map(),
                         compoundServiceErrors : Seq[String] = Seq(),
                         compoundVariablesErrors : Seq[String] = Seq(),
                         isSuccess : Boolean = false,
                         isNotFound : Boolean = false,
                         stackTrace : Option[String] = None)  {
    def hasValidationErrors = ! isSuccess && (! variablesErrors.isEmpty || ! serviceErrors.isEmpty)
    def ++(other: RequestResult) : RequestResult = {
        RequestResult(serviceErrors = this.serviceErrors ++ other.serviceErrors,
            variablesErrors = this.variablesErrors ++ other.variablesErrors,
            compoundServiceErrors = this.compoundServiceErrors ++ other.compoundServiceErrors,
            compoundVariablesErrors = this.compoundVariablesErrors ++ other.compoundVariablesErrors,
            isSuccess = this.isSuccess && other.isSuccess,
            isNotFound = this.isNotFound && other.isNotFound,
            stackTrace = other.stackTrace
        )
    }
}

sealed abstract class ExtendedResult[+S]() extends Product with Serializable {
  def map[B](f: S => B): ExtendedResult[B] = this match {
    case x: Failure => x
    case (Success(a, _)) => Success(f(a))
  }

  def ++[B >: S](r: ExtendedResult[B]) : ExtendedResult[B] = (this, r) match {
        case (Success(a, _), Success(b, _)) => Success(a)
        case (a@Failure(s, v, cs, cw, nf, _, _), b@Failure(s1, v1, cs1, cw1, nf1, _, _)) => Failure(s ++ s1, v ++ v1, cs ++ cs1, cw ++ cw1, nf || nf1)
        case (_, b@Failure(_, _, _, _, _, _, _)) => b.asInstanceOf[ExtendedResult[B]]
        case (a@Failure(_, _, _, _, _, _, _), _) => a.asInstanceOf[ExtendedResult[B]]
  }

  def get: S
}

final case class Success[+S](result: S, isSuccess: Boolean = true) extends ExtendedResult[S] {
    def get = result
}

final case class Failure(serviceErrors : Map[String, String] = Map(),
                         variablesErrors : Map[String, String] = Map(),
                         compoundServiceErrors : Seq[String] = Seq(),
                         compoundVariablesErrors : Seq[String] = Seq(),
                         isNotFound: Boolean = false,
                         isSuccess: Boolean = false,
                         stackTrace: Option[String] = None) extends ExtendedResult[Nothing] {
    def get = throw new NoSuchElementException("Failure.get")
}

case class User( @Size(min = 2, max = 32) @Pattern(regexp = "[a-z0-9.\\-_]*", message = "{validation.invalid.name}")
                 username: String,
                 @Email @Size(min = 1, max = 256)
                 email: String,
                 @Size(min = 1, max = 256) @NotBlank @Pattern(regexp = "[\\p{L} ]*", message = "{validation.invalid.person.name}")
                 firstName: String,
                 @Size(min = 1, max = 256) @NotBlank @Pattern(regexp = "[\\p{L} ]*", message = "{validation.invalid.person.name}")
                 lastName: String,
                 @OptString(max = 128, notBlank = true)
                 jobTitle: Option[String],
                 @OptString(notBlank = true, min=3, max=64)
                 password: Option[String],
                 groups: Option[Seq[String]] = None)

case class UserGroup( @Size(min = 2, max = 32) @Pattern(regexp = "[a-z0-9.\\-_]*", message = "{validation.invalid.name}")
                      name: String,
                      @NotBlank
                      description: String,
                      @OptEmail
                      mailingList: Option[String],
                      id: Option[Int] = None,
                      users: Option[Seq[String]] = None)

case class Project( id: Option[Int],
                    name: String,
                    creator: String,
                    creationTime: Long,
                    projectManager: String,
                    description: Option[String],
                    isDeleted: Boolean = false,
                    removalTime: Option[Long] = None ) extends Identifiable[Option[Int]]


case class ProjectAttributes (@Size(min = 1, max = 64) @NotBlank @Pattern(regexp = "[\\p{L}0-9.\\-_ ]*", message = "{validation.invalid.title}")
                             name: String,
                             @Size(min = 2, max = 128) @NotBlank @Pattern(regexp = "[\\p{L} ]*", message = "{validation.invalid.person.name}")
                             projectManager: String,
                             description: Option[String] )

case class ConfigProperty(name: String, value: String, readOnly: Boolean, description: Option[String] = None, propertyType: ConfigPropertyType.ConfigPropertyType = ConfigPropertyType.TEXT  )

object ConfigPropertyType extends Enumeration {
  type ConfigPropertyType = Value
  val TEXT = Value(0, "text")
  val PASSWORD = Value(1, "password")
}

case class DataItem( id: Option[Int],
                     @Size(min = 1, max = 256) @NotBlank
                     name: String,
                     @Size(max = 256)
                     value: String,
                     dataBagId: Option[Int] )

case class DataBag( id: Option[Int],
                    @Size(min = 1, max = 128) @NotBlank name: String,
                    tags: Seq[String],
                    projectId: Option[Int] = None,
                    @ValidSeq items: Seq[DataItem] = Seq() )

case class Plugin(id: String, description: Option[String])

case class PluginDetails(id: String,  description: Option[String], configuration: Seq[ConfigProperty])

case class Credentials( id: Option[Int],
                        projectId: Int,
                        @Size(min = 1, max = 128) @NotBlank cloudProvider: String,
                        @Size(min = 1, max = 128) @NotBlank pairName: String,
                        @Size(min = 1, max = 128) @NotBlank identity: String,
                        credential: Option[String],
                        fingerPrint: Option[String] = None)

case class AuthorityDescription(name: String, users: List[String], groups: List[String])

case class ActionTracking(uuid: String, name: String, description: Option[String], startedTimestamp: Long, finishedTimestamp: Option[Long], status: String)

case class ServerArray( id: Option[Int],
                        projectId: Int,
                        @Size(min = 1, max = 128) @NotBlank
                        name: String,
                        @OptString(min = 0, max = 128, notBlank = true)
                        description: Option[String] ) {
}

case class Server(id: Option[Int],
                  arrayId: Int,
                  instanceId: String,
                  address: String,
                  credentialsId: Option[Int]) {
  def this(id: Option[Int], arrayId: Int, address: String, credentialsId: Option[Int]) = this(id, arrayId, util.UUID.randomUUID().toString, address, credentialsId )
}

case class LeaseDescription(envId: Int, envName: String, sinceDate: Long)

case class ServerDescription(id: Option[Int], arrayId: Int, instanceId: String, address: String, usage: Seq[LeaseDescription] )

case class TemplateRepo(mode: String, configuration: Seq[ConfigProperty], desc: Option[String] = None)

object RequestResult {
    val envName = "envName"
    val template = "template"
}

case class VcsProject(name : String, id: String)
case class Tag(p: VcsProject, name: String)

case class StepLogEntry(timestamp: Timestamp, message: String) {
  import java.text.DateFormat._

  def toString(locale: Locale, timeZone: TimeZone) = {
    val dateFormat: DateFormat = getDateTimeInstance(SHORT, MEDIUM, locale)
    dateFormat.setTimeZone(timeZone)
    "%s: %s".format(dateFormat.format(timestamp), message)
  }

  override def toString = toString(Locale.getDefault, TimeZone.getDefault)
}
