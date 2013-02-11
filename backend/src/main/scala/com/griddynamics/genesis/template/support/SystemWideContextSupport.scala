package com.griddynamics.genesis.template.support

import com.griddynamics.genesis.repository.DatabagRepository
import scala.collection.JavaConversions._
import groovy.lang.GroovyObjectSupport
import groovy.util.Expando
import java.util
import com.griddynamics.genesis.service.EnvironmentService
import com.griddynamics.genesis.api.Configuration

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


trait UnifiedDatabagSupport {
    def projectId: Int
    def databagRepository: DatabagRepository
    lazy val system = new SystemContext(databagRepository, None)
    lazy val project = new SystemContext(databagRepository, Some(projectId))
    def get$databags = new GroovyObjectSupport() {
        def getAt(property: String) = {

            new BagWrapper(system.databag(property) ++ project.databag(property))
        }
    }

}

class SystemContext(databagRepository: DatabagRepository, projectId: Option[Int] = None) {
  def databag(name: String): java.util.Map[String, String] = {
    val bag = databagRepository.findByName(name, projectId)
    if (bag == null || bag.isEmpty) {
      java.util.Collections.emptyMap()
    } else {
      val bagItems = bag.get.items.map { case item => (item.name, item.value) }
      bagItems.toMap[String, String]
    }
  }

  def getDatabag: GroovyObjectSupport = new GroovyObjectSupport {
    def getAt(property: String): Expando = {
      new BagWrapper(databag(property))
    }
  }
}

class BagWrapper(items: java.util.Map[String, String]) extends Expando(items) {
    def isEmpty: Boolean = {
        super.getProperties.isEmpty
    }
}