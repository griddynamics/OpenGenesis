package com.griddynamics.genesis.spring.aop

import org.aspectj.lang.annotation._
import org.aspectj.lang.{ProceedingJoinPoint, JoinPoint}
import com.griddynamics.genesis.rest.annotations.LinksTo
import com.griddynamics.genesis.rest.links.{ControllerClassAggregator, Link, WithLinks}
import javax.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import com.griddynamics.genesis.util.Logging

@Aspect
class LinksToAspect extends Logging {

  @Pointcut(value="@annotation(com.griddynamics.genesis.rest.annotations.LinksTo)")
  def linksTo() {}

  @Around(value = "linksTo() && @annotation(ann)")
  def postProcessMessage(joinPoint: ProceedingJoinPoint, ann: LinksTo) = {
    val proceed = joinPoint.proceed()
    if (proceed.isInstanceOf[WithLinks]) {
      LinksToAspect.getRequest(joinPoint).map(
        implicit request => {
          def objWithLinks = proceed.asInstanceOf[WithLinks]
          val links: Array[Link] = ann.value().map(a => ControllerClassAggregator(a.controller, a.clazz(), a.rel())).flatten
          objWithLinks.add(links)
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
