package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.repository.DatabagTemplateRepository
import com.griddynamics.genesis.model.{ValueMetadata, DatabagTemplate}
import com.typesafe.config.Config

/*class DatabagTemplateRepositoryImpl extends DatabagTemplateRepository {
  def list = List()
}*/

object DatabagTemplateReader {
  import ValueMetadata.get
  val databagKey = "properties"
  val idKey = "id"
  val nameKey = "name"
  val defaultKey = "databag-name"
  val scopeKey = "scope"
  val tagsKey = "default-tags"
  def read(c: Config) : DatabagTemplate = {
    import collection.JavaConversions._
    implicit val config: Config = c
    def getString(key:String, default: String) = get(key, (c,key) => c.getString(key), default)
    val tags: String = get(tagsKey, (config, key) => config.getStringList(key), java.util.Collections.emptyList[String]()).mkString(",")
    new DatabagTemplate(getString(idKey, null), getString(nameKey, ""),
      Option(getString(defaultKey, null)), getString(scopeKey, ""), tags, readProperties(c.getConfig(databagKey)))
  }

  private def readProperties(config: Config): Map[String, ValueMetadata] = {
    import collection.JavaConversions._
    config.root().keysIterator.map(key => (key -> {
      val value: Config = config.getConfig(key)
      new ValueMetadata(value)
    })).toMap
  }
}
