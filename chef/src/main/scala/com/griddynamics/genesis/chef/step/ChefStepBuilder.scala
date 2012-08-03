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
package com.griddynamics.genesis.chef.step

import com.griddynamics.genesis.util.JsonUtil
import com.griddynamics.genesis.plugin.{StepBuilder, StepBuilderFactory}
import collection.{JavaConversions => JC}
import java.util.{Collections, List => JList, Map => JMap}
import reflect.BeanProperty

class ChefRunStepBuilderFactory extends StepBuilderFactory {
    val stepName = "chefrun"

    def newStepBuilder = new StepBuilder {
        @BeanProperty var roles : JList[String] = Collections.emptyList()
        @BeanProperty var isGlobal : Boolean = false
        @BeanProperty var runList : JList[String] = Collections.emptyList()
        @BeanProperty var jattrs : JMap[Any, Any] = Collections.emptyMap()
        @BeanProperty var templates: String = null

        def getDetails = ChefRun(JC.asScalaBuffer(roles).toSet,
                                 isGlobal,
                                 JC.asScalaBuffer(runList).toSeq,
                                 JsonUtil.toJson(jattrs),
                                 Option(templates))
    }
}

class CreateChefRoleBuilderFactory extends StepBuilderFactory {
    val stepName = "chefrole"

    def newStepBuilder = new StepBuilder {
        @BeanProperty var name : String = _
        @BeanProperty var description : String = ""
        @BeanProperty var runList : JList[String] = Collections.emptyList()
        @BeanProperty var defaults : JMap[Any, Any] = Collections.emptyMap()
        @BeanProperty var overrides : JMap[Any, Any] = Collections.emptyMap()
        @BeanProperty var overwrite : Boolean = false

        def getDetails = CreateChefRole(name, description, JC.asScalaBuffer(runList).toSeq,
                                        JsonUtil.toJson(defaults), JsonUtil.toJson(overrides), overwrite)
    }
}

class CreateChefDatabagBuilderFactory extends StepBuilderFactory {
    val stepName = "chefdatabag"

    def newStepBuilder = new StepBuilder {
        @BeanProperty var name : String = _
        @BeanProperty var items : JMap[String, JMap[Any, Any]] = Collections.emptyMap()
        @BeanProperty var overwrite : Boolean = false

        def getDetails = {
            val its = for((k,v) <- JC.mapAsScalaMap(items).toMap) yield (k, JsonUtil.toJson(v))
            CreateChefDatabag(name, its, overwrite)
        }
    }
}

class DestroyChefEnvStepBuilderFactory extends StepBuilderFactory{
    def newStepBuilder = new StepBuilder {
        def getDetails = new DestroyChefEnv
    }

    val stepName = "clearChefEnv"
}
