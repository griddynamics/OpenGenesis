package com.griddynamics.genesis.spring.aop

import org.aspectj.lang.annotation._
import org.aspectj.lang.ProceedingJoinPoint
import com.griddynamics.genesis.rest.annotations.{LinkTarget, AddSelfLinks, LinksTo}
import com.griddynamics.genesis.rest.links.{LinkBuilder, WebPath, ControllerClassAggregator, WithLinks}
import com.griddynamics.genesis.rest.links.HrefBuilder._
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.util.Logging
import org.springframework.security.access.{SecurityMetadataSource, AccessDecisionManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import com.griddynamics.genesis.spring.security.LinkSecurityBean
import com.griddynamics.genesis.api.Link

@Aspect
class LinksToAspect extends Logging {

  @Autowired
  var context: ApplicationContext = _
  var manager: Option[AccessDecisionManager] = None
  var securityMetadataSource: Option[SecurityMetadataSource] = None

  @Autowired
  var linkSecurity: LinkSecurityBean = _

  @Pointcut(value="@annotation(com.griddynamics.genesis.rest.annotations.LinksTo)")
  def linksTo() {}

  @Pointcut(value="@annotation(com.griddynamics.genesis.rest.annotations.AddSelfLinks)")
  def addSelfLinks() {}

  @Around(value = "linksTo() && @annotation(ann)")
  def postProcessMessage(joinPoint: ProceedingJoinPoint, ann: LinksTo) = {
    val proceed = joinPoint.proceed()
    if (proceed.isInstanceOf[WithLinks]) {
      LinksToAspect.getRequest(joinPoint).map(
        implicit request => {
          def objWithLinks = proceed.asInstanceOf[WithLinks]
          val links: Array[Link] = ann.value().map(a => ControllerClassAggregator(a.controller, a.clazz(), a.rel(), a.methods())).flatten
          objWithLinks.add(linkSecurity.filter(links))
        }
      ).getOrElse({
        log.warn("Cannot find request in arguments of method annotated with @LinksTo")
        proceed
      })
    } else {
      proceed
    }
  }

  @Around(value = "addSelfLinks() && @annotation(ann)")
  def appendLinks(joinPoint: ProceedingJoinPoint, ann: AddSelfLinks) = {
    val proceed = joinPoint.proceed()
    if (proceed.isInstanceOf[WithLinks]) {
       LinksToAspect.getRequest(joinPoint).map(
          implicit request => {
            def wrapper = proceed.asInstanceOf[WithLinks]
            val top = WebPath(request)
            val link = Array(LinkBuilder(top, LinkTarget.SELF, ann.modelClass(), ann.methods() : _*))
            wrapper.add(linkSecurity.filter(link))
          }
       ).getOrElse({
         log.warn("Cannot find request in arguments of method annotated with %s", ann.getClass)
       })
    } else {
      val actualClass = if (proceed != null)
        proceed.getClass
      else
        Void.TYPE
      log.warn("Unsupported result for method annotated with %s. Expected one of WithLinks actually: %s", ann.getClass, actualClass)
      proceed
    }
  }
}

object LinksToAspect {
  implicit def getRequest(joinPoint: ProceedingJoinPoint) : Option[HttpServletRequest] = {
    joinPoint.getArgs.collectFirst({case r: HttpServletRequest => r})
  }
}
