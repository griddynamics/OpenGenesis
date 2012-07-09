package com.griddynamics.genesis.repository

import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.api.{DataItem, DataBag}
import org.squeryl.PrimitiveTypeMode._
import com.griddynamics.genesis.model.DataBagItem

trait DatabagRepository extends Repository[DataBag]{

  def findByTags(tags: Seq[String]): Seq[DataBag]

  def getItems(bagId: Int): Seq[DataItem]

  def deleteItem(bagId: Int, keys: List[String]): Int

  def updateItems(bagId: Int, items: Seq[DataItem])

  def findByName(name: String): Option[DataBag]
}