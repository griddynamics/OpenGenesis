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
import util.{TimeZone, Locale}
import java.text.DateFormat


trait Identifiable[T] {
  def id: T
}

trait ProjectBound {
  def projectId: Int
}

case class Configuration( id: Option[Int],
                          @Size(min = 1, max = 128) @NotBlank @Pattern(regexp = "[\\p{L}0-9.\\-_ ]*", message = "{validation.invalid.title}")
                          name: String,
                          projectId: Int,
                          description: Option[String],
                          @ValidStringMap(key_min = 1, key_max = 256, value_min = 0, value_max = 256)
                          items: Map[String, String] = Map(),
                          instanceCount: Option[Int] = None) extends Identifiable[Option[Int]] with ProjectBound

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
                       attributes: Map[String, Attribute],
                       configuration: String) extends Identifiable[Int]

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
                              currentWorkflowFinishedActionsCount: Int,
                              workflowCompleted: Option[Double],
                              attributes: Map[String, Attribute] = Map(),
                              configuration: String,
                              timeToLive: Option[Long])

case class Variable(name : String, `type`: String, description : String, optional: Boolean = false, defaultValue: String = null,
                    values:Map[String,String] = Map(), dependsOn: Option[List[String]] = None, group : Option[String] = None)

case class Template(name : String, version : String, createWorkflow : Workflow, workflows : Seq[Workflow])

case class Workflow(name : String, variables : Seq[Variable])

case class WorkflowStep(stepId: String, phase: String, status : String, details : String, started: Option[Long],
                        finished: Option[Long], title: Option[String], regular: Boolean)

case class WorkflowDetails(id: Int,
                           name : String,
                           status: String,
                           startedBy: String,
                           variables: Map[String, String],
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

sealed abstract class ExtendedResult[+S]() extends Product with Serializable {
  def map[B](f: S => B): ExtendedResult[B] = this match {
    case x: Failure => x
    case (Success(a)) => Success(f(a))
  }

  def flatMap[B](f: S => ExtendedResult[B]): ExtendedResult[B] = this match {
      case x: Failure => x
      case Success(a) => f(a)
  }

  def ++[B >: S](r: ExtendedResult[B]) : ExtendedResult[B] = (this, r) match {
        case (Success(a), Success(b)) => Success(a)
        case (a@Failure(s, v, cs, cw, nf, _), b@Failure(s1, v1, cs1, cw1, nf1, _)) => Failure(s ++ s1, v ++ v1, cs ++ cs1, cw ++ cw1, nf || nf1)
        case (_, b@Failure(_, _, _, _, _, _)) => b.asInstanceOf[ExtendedResult[B]]
        case (a@Failure(_, _, _, _, _, _), _) => a.asInstanceOf[ExtendedResult[B]]
  }

  def get: S

  final def getOrElse[B >: S](default: => B): B =
    if (isSuccess) this.get else default

  def isSuccess: Boolean
}

final case class Success[+S](result: S) extends ExtendedResult[S] {
  def get = result
  override val isSuccess = true
}

final case class Failure(serviceErrors : Map[String, String] = Map(),
                         variablesErrors : Map[String, String] = Map(),
                         compoundServiceErrors : Seq[String] = Seq(),
                         compoundVariablesErrors : Seq[String] = Seq(),
                         isNotFound: Boolean = false,
                         stackTrace: Option[String] = None) extends ExtendedResult[Nothing] {
  def get = throw new NoSuchElementException("Failure.get")

  override val isSuccess = false
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

case class ConfigProperty(name: String,
                          value: String,
                          readOnly: Boolean,
                          description: Option[String] = None,
                          propertyType: ConfigPropertyType.ConfigPropertyType = ConfigPropertyType.TEXT,
                          restartRequired: Boolean = false)

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
                        fingerPrint: Option[String] = None) extends ProjectBound

case class AuthorityDescription(name: String, users: List[String], groups: List[String])

case class ActionTracking(uuid: String, name: String, description: Option[String], startedTimestamp: Long, finishedTimestamp: Option[Long], status: String)

case class ServerArray( id: Option[Int],
                        projectId: Int,
                        @Size(min = 1, max = 128) @NotBlank
                        name: String,
                        @OptString(min = 0, max = 128, notBlank = true)
                        description: Option[String] ) extends ProjectBound

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

case class RemoteAgent(id: Option[Int],
                       @NotBlank
                       @Pattern(message = "{validation.invalid.host}",
                           regexp="^(?:(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|(?:(([a-zA-Z]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))$")
                       hostname: String,
                       @Min(1) @Max(32767) port: Int,
                       @ValidSeq tags: Seq[String],
                       lastTimeAlive: Option[Long],
                       status: Option[AgentStatus.AgentStatus] = None,
                       stats: Option[JobStats] = None
                      )


object AgentStatus extends Enumeration {
  type AgentStatus = Value
  val Connected = Value(0, "Connected")
  val Active = Value(1, "Active")
  val Disconnected = Value(2, "Disconnected")
  val Unavailable = Value(3, "Unavailable")
  val Error = Value(4, "Error")
}

case class JobStats(runningJobs: Int, totalJobs: Int)

case class Link(href: String, rel: String, `type`: Option[String], methods: Array[String] = Array())  {
  def disassemble: Array[Link] = methods.map(m => new Link(href, rel, `type`, Array(m)))
  def remove(method: String) = new Link(href, rel, `type`, methods.filter(_ != method))
}

object Links {
  def merge(links: Array[Link]) = {
    val groupedByHref = links.groupBy(_.href)

    def assembleLinkList(linksList: Array[Link], href: String): Link = {
      val firstLink = linksList(0)
      val methods = linksList.map(_.methods).flatten
      Link(href, firstLink.rel, firstLink.`type`, methods)
    }

    groupedByHref.map {
      case (href, linksList) =>  assembleLinkList(linksList, href)
    }
  }
}

case class SystemSettings(links: Array[Link])

case class ApplicationRole(name: String)

case class Access(users: Array[User], groups: Array[String])

case class Action(name: String)

object CancelAction extends Action("cancel")
object ResetAction extends Action("reset")