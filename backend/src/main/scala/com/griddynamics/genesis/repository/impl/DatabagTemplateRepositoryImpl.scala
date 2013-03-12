package com.griddynamics.genesis.repository.impl

import com.griddynamics.genesis.model.{TemplateProperty, ValueMetadata, DatabagTemplate}
import com.typesafe.config.{ConfigFactory, ConfigException, Config}
import scala.util.control.Exception._
import com.griddynamics.genesis.repository.DatabagTemplateRepository
import com.griddynamics.genesis.template.{Storage, ClasspathStorage, FilesystemStorage}

class DatabagTemplateRepositoryImpl(val path: String, val wildcard: String) extends DatabagTemplateRepository {
  val defaultClassloader = this.getClass.getClassLoader
  val storages: List[Storage[_]] =
    List(new FilesystemStorage(path, wildcard), new ClasspathStorage(defaultClassloader, wildcard, "UTF8"))
  def list = {
    val result: List[DatabagTemplate] = storages.foldLeft(List.empty[DatabagTemplate])((acc, storage) => {
      acc ++ (if (acc.isEmpty) getTemplates(storage) else List.empty[DatabagTemplate])
    })
    result
  }

  private def getTemplates[U](storage: Storage[U]) = {
    failAsValue(classOf[IllegalArgumentException])(List()) { storage.files.map(file => {
      val config = ConfigFactory.parseString(storage.content(file))
      DatabagTemplateReader.read(config)
    })
    }
  }
}

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

  private def readProperties(config: Config): Map[String, TemplateProperty] = {
    import collection.JavaConversions._
    config.root().keysIterator.map(key => (key -> {
      val value: Config = config.getConfig(key)
      new TemplateProperty(value)
    })).toMap
  }
}
