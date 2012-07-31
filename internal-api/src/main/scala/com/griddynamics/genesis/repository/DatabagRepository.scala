package com.griddynamics.genesis.repository

import com.griddynamics.genesis.api.{DataItem, DataBag}
import com.griddynamics.genesis.api

trait DatabagRepository extends Repository[DataBag]{

  def findByTags(tags: Seq[String], projectId: Option[Int] = None): Seq[DataBag]

  def getItems(bagId: Int): Seq[DataItem]

  def deleteItem(bagId: Int, keys: List[String]): Int

  def updateItems(bagId: Int, items: Seq[DataItem]): Seq[api.DataItem]

  def findByName(name: String, projectId: Option[Int] = None): Option[DataBag]

  def find(bagId: Int, projectId: Option[Int]): Option[DataBag]

  def list(projectId: Option[Int]): List[DataBag]
}