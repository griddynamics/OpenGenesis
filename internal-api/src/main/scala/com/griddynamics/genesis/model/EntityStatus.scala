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
package com.griddynamics.genesis.model

sealed abstract class EnvStatus

object EnvStatus {
    case class Requested(workflow : String) extends EnvStatus

    case class Ready() extends EnvStatus

    case class Destroyed() extends EnvStatus

    case class Executing(workflow : String) extends EnvStatus

    case class Canceled(workflow : String) extends EnvStatus

    case class Failed(workflow : String) extends EnvStatus

    private val pattern = "^(.*)\\((.*)\\)$".r

    def fromString(input: String): Option[EnvStatus] = input match {
        case pattern("Ready", "") => Some(Ready())
        case pattern("Destroyed", "") => Some(Destroyed())
        case pattern("Requested", workflow) => Some(Requested(workflow))
        case pattern("Executing", workflow) => Some(Executing(workflow))
        case pattern("Canceled", workflow) => Some(Canceled(workflow))
        case pattern("Failed", workflow) => Some(Failed(workflow))
        case _ => None
    }
}


object WorkflowStatus extends Enumeration {
    type WorkflowStatus = Value

    val Requested = Value(0, "Requested")
    val Executed = Value(1, "Executed")

    val Failed = Value(2, "Failed")
    val Succeed = Value(3, "Succeed")
    val Canceled = Value(4, "Canceled")
    val Suspended = Value(5, "Suspended")
}

object VmStatus extends Enumeration {
    type VmStatus = Value
    val Provision = Value(0, "Provision")
    val Ready = Value(1, "Ready")
    val Failed = Value(2, "Failed")
    val Destruction = Value(3, "Destruction")
    val Destroyed = Value(4, "Destroyed")
}

object MachineStatus extends Enumeration {
  type MachineStatus = Value

  val Ready = Value(0, "Ready")
  val Released = Value(1, "Released")
}

object WorkflowStepStatus extends Enumeration {
    type WorkflowStepStatus = Value

    val Requested = Value(0, "Requested")
    val Executing = Value(1, "Executing")
    val Failed = Value(2, "Failed")
    val Succeed = Value(3, "Succeed")
    val Canceled = Value(4, "Canceled")
}
