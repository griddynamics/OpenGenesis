package com.griddynamics.genesis.rest.links

import javax.servlet.http.HttpServletRequest


trait WithLinks {
  def links: Iterable[Link]
  def add(links: Iterable[Link]) : WithLinks
}

case class CollectionWrapper[T](items: Iterable[T], links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(items = items, links = coll)
  def withItems(newItems: Iterable[T]) = copy(items = newItems, links = links)
  def withLinks(link: Link, rest: Link*) = copy(items = items, links = links ++ (link :: rest.toList))
}

case class ItemWrapper[T](item: T, links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(item = item, links = coll)
  def withLinks(link: Link, rest: Link*) = copy(item = item, links = links ++ (link :: rest.toList))
}


object CollectionWrapper {
  implicit def wrap[T](coll: Iterable[T]) = new CollectionWrapper[T](coll, List())
}

object ItemWrapper {
  implicit def wrap[T](item: T) = ItemWrapper[T](item, List())
}