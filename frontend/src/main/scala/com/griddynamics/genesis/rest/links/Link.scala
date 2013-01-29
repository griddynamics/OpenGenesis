package com.griddynamics.genesis.rest.links

import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import org.springframework.web.util.UriComponentsBuilder
import com.griddynamics.genesis.http.TunnelFilter
import com.griddynamics.genesis.rest.annotations.LinkTarget

case class Link(href: String, rel: String, `type`: Option[String], methods: Array[String] = Array())  {
  def disassemble: Array[Link] = methods.map(m => new Link(href, rel, `type`, Array(m)))
  def remove(method: String) = new Link(href, rel, `type`, methods.filter(_ != method))
}

object Links {
  def merge(links: Array[Link]) = {
    val groupedByHref = links.groupBy(_.href)

    def assembleLinkList(linksList: Array[Link], href: String): Link = {
      val firstLink = linksList(0)
      val methods = linksList.map(_.methods).flatten
      Link(href, firstLink.rel, firstLink.`type`, methods)
    }

    groupedByHref.map {
      case (href, linksList) =>  assembleLinkList(linksList, href)
    }
  }
}

case class WebPath(val start: String, val elements: List[String] = List()) {
  def / (path: String) = WebPath(start, elements ++ (path :: Nil))
  override def toString() = (start :: elements).mkString("/")
}

object Link {
  def apply(href: String, rel: LinkTarget, method: RequestMethod) = new Link(href, rel.toRel, None, Array(method.toString.toLowerCase))
  def apply(href: String, rel: LinkTarget, modelClazz: Class[_], methods: RequestMethod*) =
    new Link(href, rel.toRel, modelClazz, (methods.toList).map(_.toString.toLowerCase()).toArray)
  implicit def toContentType(modelClazz: Class[_]): Some[String] = {
    Some(s"application/vnd.griddynamics.genesis.${modelClazz.getSimpleName}+json")
  }
}

object HrefBuilder {
  def absolutePath(localPath: String)(implicit request: HttpServletRequest): String = uriBuilder.path(localPath).build().toString
  implicit def duplicate(request: HttpServletRequest) = {
    implicit val req: HttpServletRequest = request
    absolutePath(request.getServletPath)
  }

  def withPathParam(request: HttpServletRequest, pathParam: Any) = {
    implicit val req: HttpServletRequest = request
    absolutePath(request.getServletPath.stripSuffix("/") + "/" + pathParam.toString)
  }

  implicit def webPathToString(path: WebPath) = path.toString


  private[links] def uriBuilder(implicit request: HttpServletRequest): UriComponentsBuilder = {
    request.getHeader(TunnelFilter.FORWARDED_HEADER) match {
      case s: String => UriComponentsBuilder.fromHttpUrl(s)
      case _ => ServletUriComponentsBuilder.fromContextPath(request)
    }
  }
}

object ControllerClassAggregator {
  import Link._
  def apply(controllerClazz: Class[_], modelClazz: Class[_], rel: LinkTarget, methods: Array[RequestMethod] = Array())(implicit request: HttpServletRequest): Array[Link] =  {
    val builder = HrefBuilder.uriBuilder

    Option(controllerClazz.getAnnotation(classOf[RequestMapping])).map(ann => {
      ann.value().map ( mapping => {
        val link: String = builder.path(mapping).build().toUriString
        val method: Array[RequestMethod] = if (methods.isEmpty)
          ann.method()
        else
          methods
        Link(link, rel.toRel, modelClazz, getMethods(method))
      })
    }).getOrElse(Array())
  }

  private[links] def getMethods(methods: Array[RequestMethod]) : Array[String] =
    if (methods.isEmpty)
      Array(RequestMethod.GET.toString.toLowerCase)
    else
      methods.map(_.toString.toLowerCase)

}
