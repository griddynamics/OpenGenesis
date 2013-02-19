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
 */package com.griddynamics.genesis.logging

import org.aopalliance.intercept.{MethodInvocation, MethodInterceptor}
import org.springframework.aop.framework.ProxyFactory
import com.griddynamics.genesis.util.{StringUtils, Logging}
import com.griddynamics.genesis.annotation.GatewayCall
import java.lang.reflect.Method

object RemoteGateway extends Logging {

  val isTraceEnabled = log.isTraceEnabled

  def apply[T](obj: T, gatewayName: String): T = {
    val proxy = new ProxyFactory(obj)

    proxy.addAdvice(new MethodInterceptor {
      def invoke(invocation: MethodInvocation) = {
        try {
          val result = invocation.proceed()
          if (isTraceEnabled) {
            log.trace(s"Remote call '$gatewayName - ${callName(invocation)}' succeeded")
          }
          result
        } catch {
          case e: Throwable =>
            val call = callName(invocation)
            log.warn(s"Remote call '$gatewayName - $call' throwed exception: ${e.getMessage}")
            throw e
        }
      }
    })

    proxy.getProxy.asInstanceOf[T]
  }


  def callName[T](invocation: MethodInvocation): String = {
    import StringUtils._
    val method: Method = invocation.getMethod
    val call = Option(method.getAnnotation(classOf[GatewayCall])).map(_.desc()).getOrElse(method.getName)
    splitByCase(call).toLowerCase
  }
}
