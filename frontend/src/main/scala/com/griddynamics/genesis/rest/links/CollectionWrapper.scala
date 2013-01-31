package com.griddynamics.genesis.rest.links

import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import HrefBuilder.duplicate
import com.griddynamics.genesis.rest.annotations.LinkTarget
import org.springframework.web.bind.annotation.RequestMethod
import com.griddynamics.genesis.api.Link


trait WithLinks {
  def links: Iterable[Link]
  def add(links: Iterable[Link]) : WithLinks
  def filtered()(implicit security: LinkSecurityBean): WithLinks
}

case class CollectionWrapper[T](items: Iterable[T], links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(items = items, links = links ++ coll)
  def withItems(newItems: Iterable[T]) = copy(items = newItems, links = links)
  def withLinks(link: Link, rest: Link*) = copy(items = items, links = links ++ (link :: rest.toList))
  def withLinksToSelf(clazz: Class[_], method: RequestMethod, methods: RequestMethod*)(implicit request: HttpServletRequest) =
    copy(items = items, links = LinkBuilder(request, LinkTarget.SELF, clazz, (method :: methods.toList).toArray : _*) :: Nil)
  override def filtered()(implicit security: LinkSecurityBean) =  copy(items = items, links = security.filter(links.toArray))
}

case class ItemWrapper[T](item: T, links: Iterable[Link]) extends WithLinks {
  override def add(coll: Iterable[Link]) = copy(item = item, links = links ++ coll)
  implicit def withLinks(link: Link, rest: Link*) = copy(item = item, links = links ++ (link :: rest.toList))
  override def filtered()(implicit security: LinkSecurityBean) =  copy(item = item, links = security.filter(links.toArray))
  def withLinksToSelf(method: RequestMethod, methods: RequestMethod*)(implicit request: HttpServletRequest) =
    copy(item = item, links = LinkBuilder(request, LinkTarget.SELF, item.getClass, (method :: methods.toList).toArray : _*) :: Nil)
}


object CollectionWrapper {
  implicit def wrapCollection[T](coll: Iterable[T]) = new CollectionWrapper[T](coll, List())
  implicit def wrap[T](item: T) = ItemWrapper[T](item, List())
}

