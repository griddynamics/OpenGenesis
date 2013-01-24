package com.griddynamics.genesis.rest.links

import javax.servlet.http.HttpServletRequest
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import org.springframework.web.util.UriComponentsBuilder
import com.griddynamics.genesis.http.TunnelFilter
import com.griddynamics.genesis.rest.annotations.LinkTarget

case class Link(href: String, rel: String, `type`: String, methods: Array[String] = Array())

object ControllerClassAggregator {
  def apply(controllerClazz: Class[_], modelClazz: Class[_], rel: LinkTarget)(implicit request: HttpServletRequest) =  {
    val builder: UriComponentsBuilder = request.getHeader(TunnelFilter.FORWARDED_HEADER) match {
      case s: String => UriComponentsBuilder.fromHttpUrl(s)
      case _ => ServletUriComponentsBuilder.fromContextPath(request)
    }

    Option(controllerClazz.getAnnotation(classOf[RequestMapping])).map(ann => {
      ann.value().map ( mapping => {
        val link: String = builder.path(mapping).build().toUriString
        Link(link, rel.toRel, s"application/vnd.griddynamics.genesis.${modelClazz.getSimpleName}+json", getMethods(ann.method()))
      })
    }).getOrElse(Array())
  }

  private[links] def getMethods(methods: Array[RequestMethod]) : Array[String] =
    if (methods.isEmpty)
      Array(RequestMethod.GET.toString.toLowerCase)
    else
      methods.map(_.toString.toLowerCase)

}
