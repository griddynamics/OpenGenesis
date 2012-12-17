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

package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.{model, api}
import api.RemoteAgent
import com.griddynamics.genesis.repository.{RemoteAgentRepository, AbstractGenericRepository}
import model.GenesisSchema
import java.sql.Timestamp
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.ast.BinaryOperatorNodeLogicalBoolean
import org.springframework.transaction.annotation.Transactional

class RemoteAgentRepositoryImpl extends AbstractGenericRepository[model.RemoteAgent, api.RemoteAgent](GenesisSchema.remoteAgents) with RemoteAgentRepository {
  implicit def convert(m: model.RemoteAgent): RemoteAgent =
    RemoteAgent(Some(m.id), m.host, m.port, m.tags.trim.toLowerCase.split(" "), Some(m.lastTimeAlive.getTime))

  implicit def convert(ent: api.RemoteAgent): model.RemoteAgent =
    new model.RemoteAgent(ent.id.getOrElse(0), ent.hostname, ent.port, ent.tags.distinct.mkString(" "), new Timestamp(0))


  def findByTags(tags: Seq[String]): Seq[RemoteAgent] = from(table)(agent => {
    val tags_has = { s: String => agent.tags like ("%" + s.toLowerCase + "%") }
    val alwaysTrue: BinaryOperatorNodeLogicalBoolean = 1 === 1
    where(tags.foldLeft(alwaysTrue) { case (acc, tag) => ( tags_has (tag) and acc) }) select (agent)
  }).toSeq.map(convert _)

  @Transactional
  override def update(agent: api.RemoteAgent) = {
    println(agent)
    table.update(
      a => where(a.id === agent.id)
        set(
        a.host := agent.hostname,
        a.port := agent.port,
        a.tags := agent.tags.distinct.mkString(" ")
        )
    )
    agent
  }

  @Transactional
  override def delete(dto: api.RemoteAgent) = {
    dto.id.map(a => {
      table.deleteWhere(b => b.id === a )
    }).getOrElse(0)
  }

  def touch(key: Int) = {
    table.update(
      a => where(a.id === key)
        set(
        a.lastTimeAlive := new Timestamp(new java.util.Date().getTime)
        )
    )
  }
}
