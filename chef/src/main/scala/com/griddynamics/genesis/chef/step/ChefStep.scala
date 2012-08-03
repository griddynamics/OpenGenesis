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


import net.liftweb.json.JsonAST.JObject
import com.griddynamics.genesis.plugin.RoleStep
import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.util.Describer

sealed trait ChefStep extends Step

case class ChefRun(roles: Set[String],
                   isGlobal: Boolean,
                   runList: Seq[String] = Seq(),
                   jattrs: JObject = JObject(List()),
                   templates: Option[String]) extends ChefStep with RoleStep {

  override val stepDescription = new Describer("Chef recipe execution").param("recipes", runList).param("templates", templates).describe
}

case class CreateChefDatabag(databag: String,
                             items: Map[String, JObject] = Map(),
                             overwrite : Boolean) extends ChefStep {

  override val stepDescription = new Describer("Chef databag creation").param("databag", databag).describe
}

case class CreateChefRole(role: String,
                          description: String = "",
                          runList: Seq[String] = Seq(),
                          defaults: JObject = JObject(List()),
                          overrides: JObject = JObject(List()),
                          overwrite : Boolean) extends ChefStep  {
  override val stepDescription = new Describer("Chef role creation")
    .param("role", role)
    .param("receipes", runList)
    .describe

}

case class DestroyChefEnv() extends ChefStep {
  override val stepDescription = "Chef environment destruction"
}
