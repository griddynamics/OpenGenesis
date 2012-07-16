package com.griddynamics.genesis.template.support

import com.griddynamics.genesis.repository.DatabagRepository
import scala.collection.JavaConversions._
import groovy.lang.GroovyObjectSupport
import groovy.util.Expando

trait SystemWideContextSupport {

  protected def databagRepository: DatabagRepository

  lazy val systemContext = new SystemContext(databagRepository)

  def get$system = systemContext

}

trait ProjectDatabagSupport extends SystemWideContextSupport {
  protected def projectId: Int
  lazy val projectContext = new SystemContext(databagRepository, Some(projectId))
  def get$project = projectContext
}

class SystemContext(databagRepository: DatabagRepository, projectId: Option[Int] = None) {
  def databag(name: String): java.util.Map[String, String] = {
    val bag = databagRepository.findByName(name, projectId)
    if (bag.isEmpty) {
      java.util.Collections.emptyMap()
    } else {
      val bagItems = bag.get.items.getOrElse(List()).map { case item => (item.name, item.value) }
      bagItems.toMap[String, String]
    }
  }

  def getDatabag: GroovyObjectSupport = new GroovyObjectSupport {
    def getAt(property: String): Expando = {
      new Expando(databag(property))
    }
  }
}