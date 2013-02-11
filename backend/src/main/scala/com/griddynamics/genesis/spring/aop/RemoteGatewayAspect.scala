/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.spring.aop

import com.griddynamics.genesis.util.{StringUtils, Logging}
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Pointcut, Aspect}
import com.griddynamics.genesis.annotation.{GatewayCall, RemoteGateway}
import org.aspectj.lang.reflect.MethodSignature
import com.yammer.metrics.scala.Instrumented
import java.util.concurrent.TimeUnit

@Aspect
class RemoteGatewayAspect(metricsOn : Boolean) extends Logging with Instrumented {

  val isTraceEnabled = log.isTraceEnabled

  @Pointcut("within(@com.griddynamics.genesis.annotation.RemoteGateway *)")
  def beanAnnotatedWithRemoteGateway() {}

  @Pointcut("execution(public * *(..))")
  def publicMethod() {}

  @Pointcut("publicMethod() && beanAnnotatedWithRemoteGateway()")
  def publicGatewayMethod() {}

  @Around(value = "publicGatewayMethod()")
  def processGatewayCalls(joinPoint: ProceedingJoinPoint) = timeMetrics(joinPoint) {
    try {
      val result = joinPoint.proceed()
      if(isTraceEnabled) {
        val (call, gatewayName) = callMarkers(joinPoint)
        log.trace(s"Remote call '$gatewayName - $call' succeeded")
      }
      result
    } catch {
      case e: Throwable =>
        val (call, gatewayName) = callMarkers(joinPoint)
        log.warn(s"Remote call '$gatewayName - $call' throwed exception: ${e.getMessage}")
        throw e
    }
  }

  def timeMetrics[A](joinPoint: ProceedingJoinPoint)( f: => A): A = if (metricsOn) timer(joinPoint).time {f} else f

  private def callMarkers(joinPoint: ProceedingJoinPoint): (String, String) = {
    val gatewayName = joinPoint.getTarget.getClass.getAnnotation(classOf[RemoteGateway]).value()
    val call = joinPoint.getSignature match {
      case m: MethodSignature => Option(m.getMethod.getAnnotation(classOf[GatewayCall])).map(_.desc()).getOrElse(m.getName)
      case m => m.getName
    }
    (StringUtils.splitByCase(call).toLowerCase, gatewayName)
  }

  private def timer(joinPoint: ProceedingJoinPoint) = {
    val (call, gatewayName) = callMarkers(joinPoint)
    metrics.timer(call, gatewayName, TimeUnit.MILLISECONDS)
  }
}
