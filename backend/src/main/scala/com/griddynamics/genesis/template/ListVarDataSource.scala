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

package com.griddynamics.genesis.template

import scala.collection.JavaConversions._

class ListVarDataSource extends VarDataSource {
    val Key = "values"
    var values: Map[String, String] = _

    def getData = values

    def config(map: Map[String, Any])  { values = map.get(Key) match {
            case Some(s: java.lang.Iterable[AnyRef]) =>
              collection.JavaConversions.iterableAsScalaIterable(s).toArray.map(v => (v.toString, v.toString)).toMap
            case Some(s: java.util.Map[AnyRef, AnyRef]) => s.toMap.map(entry => (entry._1.toString, entry._2.toString))
            case x => Map(x.toString -> x.toString)
        }
    }
}

class DependentList extends ListVarDataSource with DependentDataSource {
    def getData(v: Any) = values.mapValues(value => v.toString.takeRight(24) + ":" + value)
    def getData(v1: Any, v2: Any) = values.mapValues(value => v1.toString.takeRight(32) + v2.toString + value)
}

class DependentListFactory extends DataSourceFactory {
    val mode = "dependentList"

    def newDataSource = new DependentList
}

class ListVarDSFactory extends DataSourceFactory {
    val mode = "staticList"

    def newDataSource = new ListVarDataSource
}
