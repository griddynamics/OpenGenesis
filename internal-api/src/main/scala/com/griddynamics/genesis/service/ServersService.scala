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
package com.griddynamics.genesis.service

import com.griddynamics.genesis.api
import api.{Server, ExtendedResult, ServerArray}

trait ServersService {
  def update(array: ServerArray): ExtendedResult[ServerArray]
  def create(array: ServerArray): ExtendedResult[ServerArray]
  def deleteServerArray(projectId: Int, id: Int): ExtendedResult[Option[_]]
  def list(projectId: Int): Seq[api.ServerArray]
  def get(projectId: Int, id: Int): Option[api.ServerArray]

  def findArrayByName(projectId: Int, name: String): Option[api.ServerArray]

  def create(server: Server): ExtendedResult[Server]
  def deleteServer(arrayId: Int, serverId: Int): ExtendedResult[Option[_]]
  def getServers(arrayId: Int): Seq[Server]
  def getServer(arrayId: Int,  serverId: Int): Option[Server]
}

