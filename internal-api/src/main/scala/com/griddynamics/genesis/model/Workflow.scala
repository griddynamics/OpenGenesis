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

import org.squeryl.customtypes.StringField
import WorkflowStatus._
import java.sql.Timestamp

//TODO make stepsCount and stepsFinished Option fields
class Workflow(val envId: Int,
               val name: String,
               val startedBy: String,
               var status: WorkflowStatus,
               var stepsCount: Int,
               var stepsFinished: Int,
               val variables: VariablesField,
               var executionStarted: Option[Timestamp],
               var executionFinished: Option[Timestamp]) extends GenesisEntity {
    def this() = this (0, "", "", Requested, 0, 0, Map[String, String](), None, None)

    def copy() = {
        val w = new Workflow(envId, name, startedBy, status, stepsCount, stepsFinished, variables, executionStarted, executionFinished)
        w.id = this.id
        w
    }

}

class VariablesField(value: String) extends StringField(value) {
    private val variables = VariablesField.unmarshal(value)
}

object VariablesField {
    private val sep = "#"

    implicit def variablesFieldToMap(v: VariablesField): Map[String, String] = v.variables

    implicit def mapToVariablesField(v: Map[String, String]): VariablesField = new VariablesField(marshal(v))

    def marshal(value: Map[String, String]) = {
        value.map(e => e._1 + sep + e._2).mkString(sep)
    }

    def unmarshal(value: String): Map[String, String] = {
        var split = value.split(sep).toBuffer
        if (split.length % 2 == 1) split += ""
        if (value.isEmpty) Map() else zipTwo(split)
    }

    private def zipTwo[T](seq: Seq[T]): Map[T, T] = {
        seq match {
            case Seq() => Map()
            case Seq(a, b, rest@_*) => Map(a -> b) ++ zipTwo(rest)
            case _ => throw new IllegalArgumentException
        }
    }
}
