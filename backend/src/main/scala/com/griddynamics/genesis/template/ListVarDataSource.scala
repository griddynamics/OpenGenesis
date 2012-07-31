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

class ListVarDataSource extends VarDataSource with DependentDataSource {
    val Key = "values"
    var values: Seq[String] = _

    def getData = values.zip(values).toMap

    def getData(v: Any) = values.zip(values.map(v.toString + ":" + _)).toMap

    def config(map: Map[String, Any])  { values = map.get(Key) match {
            case Some(s: java.lang.Iterable[AnyRef]) => collection.JavaConversions.iterableAsScalaIterable(s).toArray.map(_.toString).toSeq
            case x => Seq(x.toString)
        }
    }
}

class DependentList extends DependentDataSource {
    val Key = "values"
    var values: Seq[String] = _

    def getData(v: Any) = values.zip(values.map(v.toString.takeRight(24) + ":" + _)).toMap
    def getData(v1: Any, v2: Any) = values.zip(values.map(v1.toString.takeRight(32) + v2.toString + _)).toMap

    def getData = Map()

    def config(map: Map[String, Any])  { values = map.get(Key) match {
            case Some(s: java.lang.Iterable[AnyRef]) => collection.JavaConversions.iterableAsScalaIterable(s).toArray.map(_.toString).toSeq
            case x => Seq(x.toString)
        }
    }
}

class DependentListFactory extends DataSourceFactory {
    val mode = "dependentList"

    def newDataSource = new DependentList
}

class ListVarDSFactory extends DataSourceFactory {
    val mode = "staticList"

    def newDataSource = new ListVarDataSource
}
