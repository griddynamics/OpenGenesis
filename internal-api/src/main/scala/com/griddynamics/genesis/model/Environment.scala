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

import com.griddynamics.genesis.model.EnvStatus._
import org.squeryl.Optimistic

class Environment(val name: String,
                  var status: EnvStatus,
                  val creator: String,
                  val templateName: String,
                  var templateVersion: String,
                  val projectId: GenesisEntity.Id) extends EntityWithAttrs with Optimistic {
    def this() = this ("", Busy, "", "", "", 0)

    def copy() = {
        val env = new Environment(name, status, creator, templateName,
            templateVersion, projectId).importAttrs(this)
        env.id = this.id
        env
    }

    def deploymentAttrs: Seq[DeploymentAttribute] = {
      this.get(Environment.DeploymentAttr).getOrElse(Seq())
    }

    def deploymentAttrs_=(attrs: Seq[DeploymentAttribute]) {
      this(Environment.DeploymentAttr)  = attrs
    }
}

object Environment {
  val DeploymentAttr = EntityAttr[Seq[DeploymentAttribute]]("deployment")

}
case class DeploymentAttribute(key: String, value: String, desc: String)
