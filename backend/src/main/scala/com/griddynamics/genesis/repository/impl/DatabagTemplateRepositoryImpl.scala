package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.DatabagTemplateRepository
import com.griddynamics.genesis.model.{ValueMetadata, DatabagTemplate}
import com.typesafe.config.{ConfigException, Config}
import scala.util.control.Exception._

/*class DatabagTemplateRepositoryImpl extends DatabagTemplateRepository {
  def list = List()
}

class FileSystemTemplateStorage(val path: String, val wildcard: String) {

}

class ClassPathTemplateStorage(val path: String, val wildcard: String) */

object DatabagTemplateReader {
  val databagKey = "properties"
  val idKey = "id"
  val nameKey = "name"
  val defaultKey = "databag-name"
  val scopeKey = "scope"
  val tagsKey = "default-tags"
  def read(c: Config) : DatabagTemplate = {
    import collection.JavaConversions._
    implicit val config: Config = c
    def getString(key:String, default: String = null) = failAsValue(classOf[ConfigException])(default) {c.getString(key)}
    val tags: String = failAsValue(classOf[ConfigException])("") { c.getStringList(tagsKey).mkString(",") }
    new DatabagTemplate(getString(idKey), getString(nameKey, ""),
      Option(getString(defaultKey)), getString(scopeKey, ""), tags, readProperties(c.getConfig(databagKey)))
  }

  private def readProperties(config: Config): Map[String, ValueMetadata] = {
    import collection.JavaConversions._
    config.root().keysIterator.map(key => (key -> {
      val value: Config = config.getConfig(key)
      new ValueMetadata(value)
    })).toMap
  }
}
