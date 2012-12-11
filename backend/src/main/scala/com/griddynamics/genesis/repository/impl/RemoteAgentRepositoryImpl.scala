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

  def findByTags(tags: Seq[String]): List[RemoteAgent] = from(table)(agent => {
    val tags_has = { s: String => agent.tags like ("% " + s.toLowerCase + " %") }
    val alwaysTrue: BinaryOperatorNodeLogicalBoolean = 1 === 1
    where(tags.foldLeft(alwaysTrue) { case (acc, tag) => ( tags_has (tag) and acc) }) select (agent)
  }).toList.map(convert _)

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
