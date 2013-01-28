package com.griddynamics.genesis.spring.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.{AccessDeniedException, SecurityMetadataSource, AccessDecisionManager}
import javax.annotation.PostConstruct
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.rest.links.{Links, Link}
import java.net.URL
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.InsufficientAuthenticationException


class LinkSecurityBean extends Logging {
  @Autowired
  var context: ApplicationContext = _
  var manager: Option[AccessDecisionManager] = None
  var securityMetadataSource: Option[SecurityMetadataSource] = None

  @PostConstruct
  def initialize() {
    manager = LinkSecurityBean.findAccessDecisionManager(context)
    securityMetadataSource = LinkSecurityBean.findSecurityMetadataSource(context)
    log.debug("Manager: {%s}", manager)
    log.debug("Security Metadata Source: {%s}", securityMetadataSource)
  }


  def filter(links: Array[Link]) = {
    Links.merge(links.map(link => {
      val allowed: Array[Link] = link.disassemble.collect({
        case x if evalLink(x) => x
      })
      allowed
    }).flatten)
  }

  def evalLink(l: Link): Boolean = {
    val url = new URL(l.href)
    val fi = new FilterInvocation("/", url.getPath, l.methods(0).toUpperCase)
    try {
      securityMetadataSource.map(_.getAttributes(fi)).map(attributes => {
        log.trace("Attributes: {%s}", attributes.toArray.apply(0))
        manager.map(_.decide(SecurityContextHolder.getContext.getAuthentication, fi, attributes))
      })
      true
    } catch {
      case e: AccessDeniedException  => false
      case x: InsufficientAuthenticationException => false
    }
  }

}

object LinkSecurityBean {
  import collection.JavaConversions._

  private [LinkSecurityBean] def findAccessDecisionManager(context: ApplicationContext) = {
    var c = context
    while(c.getParent != null) {
      c = c.getParent
    }
    val manager: Option[AccessDecisionManager] = getTopContext(context).getBeansOfType(classOf[AccessDecisionManager]).map(_._2).collectFirst({
      case x: AccessDecisionManager if x.supports(classOf[FilterInvocation]) => x
    })
    manager
  }

  private [LinkSecurityBean] def findSecurityMetadataSource(context: ApplicationContext) = {
    val source: Option[SecurityMetadataSource] = getTopContext(context)
      .getBeansOfType(classOf[FilterSecurityInterceptor]).map(_._2).map(_.obtainSecurityMetadataSource())
      .collectFirst({case x: SecurityMetadataSource if x.supports(classOf[FilterInvocation]) => x})
    source
  }

  private def getTopContext(context: ApplicationContext) = {
    var c = context
    while (c.getParent != null) {
      c = c.getParent
    }
    c
  }
}
