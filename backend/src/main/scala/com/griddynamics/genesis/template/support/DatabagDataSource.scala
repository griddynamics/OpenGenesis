package com.griddynamics.genesis.template.support

import com.griddynamics.genesis.template.{DataSourceFactory, VarDataSource}
import java.util.{List => JList}
import com.griddynamics.genesis.repository.DatabagRepository
import scala.collection.JavaConversions._
import java.util

class DatabagDataSource(repository: DatabagRepository) extends VarDataSource {
  import DatabagDataSource._

  var tags: Seq[String] = List()

  def getData = {
    val bags = if (tags.isEmpty) repository.list(None) else repository.findByTags(tags)
    bags.map(bag => (bag.name, bag.name)).toMap
  }

  def config(map: Map[String, Any]) {
    val tagsList = map.getOrElse(Tags, util.Collections.emptyList())
    this.tags = tagsList match {
      case s: String => List(s)
      case list: JList[String] => list
      case _ => List()
    }
  }

}

object DatabagDataSource {
  val Tags = "tags"
}


class DatabagDataSourceFactory(repository: DatabagRepository) extends DataSourceFactory {
    val mode = "databags"

    def newDataSource = new DatabagDataSource(repository)
}
