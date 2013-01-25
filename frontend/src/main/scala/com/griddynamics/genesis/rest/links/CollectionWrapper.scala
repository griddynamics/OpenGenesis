package com.griddynamics.genesis.rest.links


trait WithLinks {
  def links: Iterable[Link]
  def add(links: Iterable[Link]) : WithLinks
}

case class CollectionWrapper[T](items: Iterable[T], links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(items = items, links = coll)
}

case class ItemWrapper[T](item: T, links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(item = item, links = coll)
}


object CollectionWrapper {
  implicit def wrap[T](coll: Iterable[T]) = new CollectionWrapper[T](coll, List())
}

object ItemWrapper {
  implicit def wrap[T](item: T) = ItemWrapper[T](item, List())
}