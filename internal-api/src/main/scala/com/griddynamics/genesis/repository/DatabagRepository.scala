package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api.{DataItem, DataBag}

trait DatabagRepository extends Repository[DataBag]{

  def findByTags(tags: Seq[String], projectId: Option[Int] = None): Seq[DataBag]

  def getItems(bagId: Int): Seq[DataItem]

  def deleteItem(bagId: Int, keys: List[String]): Int

  def updateItems(bagId: Int, items: Seq[DataItem])

  def findByName(name: String, projectId: Option[Int] = None): Option[DataBag]

  def list(projectId: Option[Int]): List[DataBag]
}