package com.griddynamics.genesis.template.support

import com.griddynamics.genesis.template.{DataSourceFactory, VarDataSource}
import java.util.{List => JList}
import com.griddynamics.genesis.repository.DatabagRepository
import scala.collection.JavaConversions._
import java.util

class DatabagDataSource(repository: DatabagRepository) extends VarDataSource {
  import DatabagDataSource._

  var tags: Seq[String] = List()
  var selector: Option[Int] = None

  def getData = {
    val bags = if (tags.isEmpty) repository.list(selector) else repository.findByTags(tags, selector)
    bags.map(bag => (bag.name, bag.name)).toMap
  }

  def config(map: Map[String, Any]) {
    val projId = map.get("projectId").map(_.asInstanceOf[Int])
    val tagsList = map.getOrElse(Tags, util.Collections.emptyList())
    val source = map.getOrElse(Source, "project")
    this.tags = tagsList match {
      case s: String => List(s)
      case list: JList[String] => list
      case _ => List()
    }
    this.selector = source match {
        case "system" => None
        case _ => projId
    }
  }

}

object DatabagDataSource {
  val Tags = "tags"
  val Source = "source"
}


class DatabagDataSourceFactory(repository: DatabagRepository) extends DataSourceFactory {
    val mode = "databags"

    def newDataSource = new DatabagDataSource(repository)
}
