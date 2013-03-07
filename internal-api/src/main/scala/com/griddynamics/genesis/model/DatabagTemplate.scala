package com.griddynamics.genesis.model

class DatabagTemplate(val id: String, val name: String, val defaultName: Option[String],
                      val scope: String, val tags: String, val values: Map[String,TemplateProperty]) {
  def databag: DataBag = new DataBag(defaultName.getOrElse(""), tags, None)
  def items: List[DataBagItem] = values.map({case (key, value) => new DataBagItem(key, value.default, 0)}).toList
}

