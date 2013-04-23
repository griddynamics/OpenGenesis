package com.griddynamics.genesis.agent

import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.agents.configuration.{ConfigurationApplied, ConfigurationResponse}
import java.util.Properties
import com.typesafe.config.{ConfigValue, ConfigObject, Config, ConfigFactory}
import com.griddynamics.genesis.service.BaseConfigService
import java.util.prefs.Preferences
import com.griddynamics.genesis.util.{Closeables, Logging}
import java.io.{File, FileOutputStream, FileInputStream}
import org.springframework.beans.factory.annotation.Value
import javax.annotation.PostConstruct
import scala.collection.mutable
import com.griddynamics.genesis.model.ValueMetadata

@Configuration
class ConfigContext {
  private val defaults = ConfigFactory.load("genesis-plugin")
  @Value("${agent.properties:agent.properties}") var propertiesPath: String = _
  @Bean def configService: SimpleConfigService = new AgentConfigService(defaults, propertiesPath)
}

trait SimpleConfigService extends BaseConfigService {
  def getConfig: Map[String, String]
  def applyConfiguration(request: Map[String,String]) : ConfigurationApplied
}

class AgentConfigService(config: Config, path: String) extends SimpleConfigService with Logging {
  val prefs: Properties = new Properties()
  import scala.collection.JavaConversions._
  val defaults = config.root().toMap.filterKeys(_.startsWith("genesis")).mapValues(
  {case co: ConfigObject => new ValueMetadata(co.toConfig)}
  )

  @PostConstruct
  def init() {
    val f = new File(path)
    if (f.exists()) {
      Closeables.using(new FileInputStream(path)) { path =>
        prefs.load(path)
      }
    }
  }

  def getConfig = {
    (for (key <- defaults.keys) yield (key, get(key, defaultFor(key)))).toMap
  }

  def applyConfiguration(request: Map[String,String]) = {
    request.map({case (key, value) => prefs.put(key, value)})
    Closeables.using(new FileOutputStream(path)) { out =>
        prefs.store(out, s"Saved on ${new java.util.Date()}")
    }
    ConfigurationApplied(success = true, restart = false)
  }

  def defaultFor(key: String) = {
    defaults.get(key).map(_.default).getOrElse("")
  }

  def get[B](key: String, default: B) = {
    Option(prefs.getProperty(key)).map( value => {
    (default match {
       case i: Int => Integer.parseInt(value)
       case l: Long => java.lang.Long.parseLong(value)
       case s: String => value
       case b: Boolean => value == "true"
       case _ => throw new IllegalArgumentException("Only int, long, boolean and string are supported")
     }).asInstanceOf[B]
    }).getOrElse(default)
  }
}
