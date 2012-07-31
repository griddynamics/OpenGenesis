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
package com.griddynamics.genesis.plugin

import reflect.BeanProperty
import java.util.{Collections, List => JList, Map => JMap}
import scala.collection.{JavaConversions => JC}
import com.griddynamics.genesis.workflow.Step

trait StepBuilderFactory {
    val stepName: String

    def newStepBuilder: StepBuilder
}

trait StepBuilder {
    @BeanProperty var phase: String = _
    @BeanProperty var precedingPhases: JList[String] = Collections.emptyList()
    @BeanProperty var ignoreFail: Boolean = false
    @BeanProperty var retryCount: Int = 0
    @BeanProperty var exportTo: JMap[String, String] = Collections.emptyMap()
    var id: Int = 0

    def newStep = new GenesisStep(id, phase, JC.asScalaBuffer(precedingPhases).toSet, ignoreFail,
        if (retryCount < 0) 0 else retryCount, getDetails, JC.mapAsScalaMap(exportTo).toMap)

    def getDetails: Step
}
