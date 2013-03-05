package com.griddynamics.genesis.model

import com.typesafe.config.{ConfigValue, ConfigException, Config}
import collection.JavaConversions._


class ValueMetadata(c: Config) {
  import ValueMetadata._
  val default = c.getString("default")
  val description = getStringOption("description")

  def getValidation = getMap("validation").mapValues(_.unwrapped.toString)

  protected def getStringOption(key: String) = ValueMetadata.get(key, (config, key) => Option(config.getString(key)), None)(c)

  protected def getBoolean(key: String) = get(key, (config, key) => config.getBoolean(key), false)(c)

  protected def getMap(key: String): Map[String, ConfigValue] = get(key, (config, key) => config.getObject(key).toMap, Map[String, ConfigValue]())(c)
}

object ValueMetadata {
  def get[T](key: String, getter: (Config, String) => T, default: T)(implicit c: Config) = try {
    getter(c, key)
  } catch {
    case m: ConfigException.Missing => default
    case n: ConfigException.Null => default
  }

  def getStringOption(key: String)(implicit c: Config) = ValueMetadata.get(key, (config, key) => Option(config.getString(key)), None)(c)
  def getBoolean(key: String)(implicit c: Config) = get(key, (config, key) => config.getBoolean(key), false)(c)
}
