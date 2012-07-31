/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.metadata

import com.griddynamics.genesis.plugin.{StepBuilderFactory, StepBuilder}
import reflect.BeanProperty
import java.util.Collections
import java.util.{Map => JMap}
import scala.collection.JavaConversions._
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.model.DeploymentAttribute

class UpdateEnvAttributesStepBuilder extends StepBuilder {

  @BeanProperty var items: JMap[String, Any] = Collections.emptyMap()

  override def getDetails = {
    val data = items.collect { case (name: String, value: JMap[String, String]) => DeploymentAttribute(name, value.head._2, value.head._1) }
    UpdateEnvAttributeStep(data.toSeq)
  }
}

class UpdateEnvAttributesStepBuilderFactory extends StepBuilderFactory {
  override val stepName = "updateEnvAttributes"

  override def newStepBuilder = new UpdateEnvAttributesStepBuilder
}

case class UpdateEnvAttributeStep(entries: Seq[DeploymentAttribute]) extends Step {
  override def stepDescription = "Update environment attributes"
}

