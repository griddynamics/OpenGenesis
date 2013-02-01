package com.griddynamics.genesis.spring.aop

import org.aspectj.lang.annotation._
import org.aspectj.lang.ProceedingJoinPoint
import com.griddynamics.genesis.rest.annotations.{LinkTarget, AddSelfLinks, LinksTo}
import com.griddynamics.genesis.rest.links._
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
  def processLinksTo(joinPoint: ProceedingJoinPoint, ann: LinksTo) = {
    addLinks(joinPoint, ann) { (ann: LinksTo, request:HttpServletRequest) => processAnnotation(ann)(request) }
  }

  @Around(value = "addSelfLinks() && @annotation(ann)")
  def processAddSelfLinks(joinPoint: ProceedingJoinPoint, ann: AddSelfLinks) = {
    addLinks(joinPoint, ann) { (ann: AddSelfLinks, request:HttpServletRequest) => processAnnotation(ann)(request) }
  }

  private def processAnnotation(ann: LinksTo)(implicit request: HttpServletRequest): Array[Link] = {
    ann.value().map(a => {
      val links: Array[Link] = a.path() match {
        case "" => ControllerClassAggregator(a.controller, a.modelClass(), a.rel(), a.methods())
        case x => Array(LinkBuilder(WebPath(request) / x, a.rel(), a.modelClass(), a.methods(): _*))
      }
      links
    }).flatten
  }

  private def processAnnotation(ann: AddSelfLinks)(implicit request: HttpServletRequest): Array[Link] = {
    val top = WebPath(request)
    val link = Array(LinkBuilder(top, LinkTarget.SELF, ann.modelClass(), ann.methods(): _*))
    link
  }

  private def addLinks[T](joinPoint: ProceedingJoinPoint, ann: T)(block: (T, HttpServletRequest) => Array[Link]): Any = {
    val proceed = joinPoint.proceed()
    if (proceed.isInstanceOf[WithLinks]) {
      LinksToAspect.getRequest(joinPoint).map(
        implicit request => {
          def wrapper = proceed.asInstanceOf[WithLinks]
          val link: Iterable[Link] = block(ann, request)
          wrapper.add(linkSecurity.filter(link.toArray))
        }
      ).getOrElse({
        log.warn("Cannot find HttpServletRequest in arguments of method annotated with %s", ann)
        proceed
      })
    } else {
      val actualClass = if (proceed != null)
        proceed.getClass
      else
        Void.TYPE
      log.warn("Unsupported result for method annotated with %s. Expected one of WithLinks, but actually it's: %s", ann, actualClass)
      proceed
    }
  }
}

object LinksToAspect {
  implicit def getRequest(joinPoint: ProceedingJoinPoint) : Option[HttpServletRequest] = {
    joinPoint.getArgs.collectFirst({case r: HttpServletRequest => r})
  }
}
