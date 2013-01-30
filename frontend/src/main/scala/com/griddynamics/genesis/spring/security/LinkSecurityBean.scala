package com.griddynamics.genesis.spring.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.{AccessDeniedException, SecurityMetadataSource, AccessDecisionManager}
import javax.annotation.PostConstruct
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor
import com.griddynamics.genesis.util.Logging
import java.net.URL
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.InsufficientAuthenticationException
import com.griddynamics.genesis.api.{Links, Link}


class LinkSecurityBean extends Logging {
  @Autowired
  var context: ApplicationContext = _
  var manager: Option[AccessDecisionManager] = None
  var securityMetadataSource: Option[SecurityMetadataSource] = None

  @PostConstruct
  def initialize() {
    manager = LinkSecurityBean.findAccessDecisionManager(context)
    securityMetadataSource = LinkSecurityBean.findSecurityMetadataSource(context)
  }


  def filter(links: Array[Link]) = {
    def removeProhibited(link: Link): Array[Link] = {
      link.disassemble.collect({
        case x if evalLink(x) => x
      })
    }
    val filteredLinks = links.map(link => {
      removeProhibited(link)
    }).flatten

    Links.merge(filteredLinks)
  }

  def evalLink(l: Link): Boolean = {
    val url = new URL(l.href)
    val fi = new FilterInvocation("/", url.getPath, l.methods(0).toUpperCase)
    try {
      val configAttributes = securityMetadataSource.map(_.getAttributes(fi))
      configAttributes.map(attributes => {
        log.trace("Evaluating whether current user can visit link {%s}. Security attributes are: {%s}", l, attributes.toArray.apply(0))
        manager.map(_.decide(SecurityContextHolder.getContext.getAuthentication, fi, attributes))
      })
      true
    } catch {
      case e: AccessDeniedException  => false
      case x: InsufficientAuthenticationException => false
    }
  }

}

object LinkSecurityBean extends Logging {
  import collection.JavaConversions._

  private [LinkSecurityBean] def findAccessDecisionManager(context: ApplicationContext) = {
    getInterceptorProperty(context) {interceptor => interceptor.getAccessDecisionManager}
  }

  private [LinkSecurityBean] def findSecurityMetadataSource(context: ApplicationContext) = {
    getInterceptorProperty(context) {interceptor => interceptor.getSecurityMetadataSource}
  }

  private def getInterceptorProperty[T <% {def supports(clazz: Class[_]): Boolean}](context: ApplicationContext)(extract: FilterSecurityInterceptor => T) = {
    val interceptors = findBeansOfType(context, classOf[FilterSecurityInterceptor])
    if (! interceptors.isEmpty) {
      val interceptorsList = interceptors.map { case (name, interceptor) => interceptor }
      val extracted = interceptorsList.map(interceptor => extract(interceptor))
      extracted.collectFirst {
        case element if element.supports(classOf[FilterInvocation]) => element
      }
    } else {
      log.warn("Cannot find any instance of FilterSecurityInterceptor in Spring context. Secure link filtering will be disabled.")
      None
    }
  }

  private def findBeansOfType[T](currentContext: ApplicationContext, clazz: Class[T]) = {
    getTopContext(currentContext).getBeansOfType(clazz)
  }

  private def getTopContext(context: ApplicationContext) = {
    var c = context
    while (c.getParent != null) {
      c = c.getParent
    }
    c
  }
}
