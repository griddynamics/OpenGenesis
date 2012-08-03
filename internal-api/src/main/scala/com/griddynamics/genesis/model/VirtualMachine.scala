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
package com.griddynamics.genesis.model

import com.griddynamics.genesis.model.VmStatus._
import com.griddynamics.genesis.model.MachineStatus._
import collection.JavaConversions._
import java.sql.Timestamp
import java.io.{IOException, ObjectInputStream}

@SerialVersionUID(6868370898183761726L)
case class IpAddresses(publicIp:Option[String] = None, privateIp:Option[String] = None) {
  var address = publicIp.getOrElse(privateIp.getOrElse(""))

  @throws(classOf[IOException])
  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    if (address == null) address = publicIp.getOrElse(privateIp.getOrElse(""))
  }
}

sealed trait EnvResource extends EntityWithAttrs {
  def envId: GenesisEntity.Id

  def workflowId: GenesisEntity.Id
  def stepId: GenesisEntity.Id

  def roleName: String
  def instanceId: Option[String]

  def copy(): EnvResource

  def isReady: Boolean

  def keyPair: Option[String]
  def keyPair_=(pair: Option[String])
  def getIp: Option[IpAddresses]
  def setIp(ip: String)
}

trait CommonAttrs { this: EntityWithAttrs =>
  import VirtualMachine._

  def getIp = this.get(IpAttr)

  def setIp(ip: String) {this(IpAttr) = IpAddresses(publicIp = Option(ip))}

  def keyPair: Option[String] = this.get(KeyPair)

  def keyPair_=(pair: Option[String]) {
    pair.foreach( this(KeyPair) = _ )
  }
}

class BorrowedMachine (val serverId: GenesisEntity.Id,
                    val instanceId: Option[String],
                    val envId: GenesisEntity.Id,
                    val workflowId: GenesisEntity.Id,
                    val stepId: GenesisEntity.Id,
                    var status: MachineStatus,
                    val roleName: String,
                    var borrowTime: java.sql.Timestamp,
                    var releaseTime: Option[java.sql.Timestamp] = None) extends EnvResource with CommonAttrs {

  def this() = this (0, Option(""), 0, 0, 0, MachineStatus.Ready, "", new Timestamp(System.currentTimeMillis()))

  def copy = {
    val cp  = new BorrowedMachine(serverId, instanceId, envId, workflowId, stepId, status, roleName, borrowTime, releaseTime).importAttrs(this)
    cp.id = id
    cp
  }

  def isReady = this.status == MachineStatus.Ready
}

class VirtualMachine(val envId: GenesisEntity.Id,
                     val workflowId: GenesisEntity.Id,
                     val stepId: GenesisEntity.Id,
                     var status: VmStatus,
                     val roleName: String,
                     var instanceId: Option[String] = None,
                     val hardwareId: Option[String] = None,
                     val imageId: Option[String] = None,
                     val cloudProvider: Option[String] = None) extends EnvResource with CommonAttrs {
  def this() = this (0, 0, 0, Provision, "")

  def copy() = {
    val vm = new VirtualMachine(envId, workflowId, stepId, status, roleName,
      instanceId, hardwareId, imageId, cloudProvider).importAttrs(this)
    vm.id = this.id
    vm
  }

  import VirtualMachine._

  def computeSettings_= (settingsOption: Option[Map[String, Any]]) {
    settingsOption.foreach { settings =>
      val props = new java.util.Properties()
      settings.foreach { case (key, value) => props.setProperty(key, value.toString) }
      this(СomputeSettings) = props
    }
  }

  def computeSettings: Option[Map[String, Any]] = this.get(СomputeSettings).map { propertiesAsScalaMap(_).toMap }

  def securityGroup: Option[String] = this.get(SecurityGroup)

  def securityGroup_=(group: Option[String]) {
    group.foreach(this(SecurityGroup) = _)
  }

  def isReady = this.status == VmStatus.Ready
}

object VirtualMachine {
  val IpAttr = EntityAttr[IpAddresses]("ip")
  val СomputeSettings = EntityAttr[java.util.Properties]("compute")
  val KeyPair = EntityAttr[String]("keypair")
  val SecurityGroup = EntityAttr[String]("securityGroup")
}