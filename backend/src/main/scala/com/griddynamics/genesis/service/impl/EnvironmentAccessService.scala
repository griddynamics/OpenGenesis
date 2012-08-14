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
 * Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.api.Environment
import com.griddynamics.genesis.service
import org.springframework.security.acls.domain.{BasePermission, ObjectIdentityImpl}
import org.springframework.beans.factory.annotation.Value

class EnvironmentAccessService(permissionService: PermissionService) extends service.EnvironmentAccessService {

  @Value("${genesis.system.security.environment.restriction.enabled:false}")
  var securityEnabled: Boolean = _

  def getAccessGrantees(envId: Int): (Iterable[String], Iterable[String]) = {   //(usernames, groupnames)
    val oi = new ObjectIdentityImpl(classOf[Environment], envId)
    permissionService.getPermissionAssignees(oi, BasePermission.READ)
  }

  def grantAccess(envId: Int, users: List[String], groups: List[String]) {
    val oi = new ObjectIdentityImpl(classOf[Environment], envId)
    permissionService.grantObjectPermission(oi, BasePermission.READ, users, groups)
  }

  def restrictionsEnabled = securityEnabled
}
