package com.griddynamics.genesis.spring.aop

import org.aspectj.lang.annotation._
import org.aspectj.lang.ProceedingJoinPoint
import com.griddynamics.genesis.rest.annotations.LinksTo
import com.griddynamics.genesis.rest.links.{ControllerClassAggregator, Link, WithLinks}
import javax.servlet.http.HttpServletRequest
import com.griddynamics.genesis.util.Logging
import org.springframework.security.access.{SecurityMetadataSource, AccessDecisionManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import com.griddynamics.genesis.spring.security.LinkSecurityBean

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
}

object LinksToAspect {
  implicit def getRequest(joinPoint: ProceedingJoinPoint) : Option[HttpServletRequest] = {
    joinPoint.getArgs.collectFirst({case r: HttpServletRequest => r})
  }
}
