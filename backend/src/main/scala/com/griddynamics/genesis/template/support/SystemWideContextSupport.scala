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

class SystemContext(databagRepository: DatabagRepository) {
  def databag(name: String): java.util.Map[String, String] = {
    val bag = databagRepository.findByName(name)
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