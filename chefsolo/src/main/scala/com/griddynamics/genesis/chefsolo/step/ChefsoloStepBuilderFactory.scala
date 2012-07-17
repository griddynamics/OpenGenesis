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
package com.griddynamics.genesis.chefsolo.step

import com.griddynamics.genesis.plugin.{StepBuilder, StepBuilderFactory}
import reflect.BeanProperty
import java.util.{Collections, Map => JMap, List => JList}
import com.griddynamics.genesis.util.JsonUtil


class ChefsoloStepBuilderFactory extends StepBuilderFactory {
  val stepName = "chefsolo"

  def newStepBuilder = {
    new StepBuilder() {
      @BeanProperty var dependsOn: Array[String] = _
      @BeanProperty var roles: JList[String] = Collections.emptyList()
      @BeanProperty var ipAddress: String = _
      @BeanProperty var jattrs : JMap[Any, Any] = Collections.emptyMap()
      @BeanProperty var cookbooks: String = _
      import collection.JavaConversions._
      def getDetails = new ChefsoloRunStep(roles.toList, dependsOn, Option(ipAddress),
          JsonUtil.toJson(jattrs), cookbooks)
    }
  }
}
