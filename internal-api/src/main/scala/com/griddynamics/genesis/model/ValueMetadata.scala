package com.griddynamics.genesis.model

import com.typesafe.config.{ConfigValue, ConfigException, Config}
import collection.JavaConversions._
import scala.util.control.Exception._


class ValueMetadata(c: Config) {
  val default = c.getString("default")
  val description = getStringOption("description")

  def getValidation = getMap("validation").mapValues(_.unwrapped.toString)

  protected def getStringOption(key: String): Option[String] = failing(classOf[ConfigException]) { Option(c.getString(key)) }

  protected def getBoolean(key: String): Boolean = failAsValue(classOf[ConfigException])(false) { c.getBoolean(key) }

  protected def getMap(key: String): Map[String, ConfigValue] =
    failAsValue(classOf[ConfigException])(Map.empty[String, ConfigValue]) { c.getObject(key).toMap }
}

class TemplateProperty(c: Config) extends ValueMetadata(c) {
  val required = getBoolean("required")
  override def toString = getValidation.toString()
}
