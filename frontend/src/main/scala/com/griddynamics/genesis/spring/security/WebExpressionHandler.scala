/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.spring.security

import org.springframework.security.core.Authentication
import org.springframework.security.web.FilterInvocation
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.expression.SecurityExpressionRoot
import org.springframework.security.web.access.expression.{WebSecurityExpressionRoot, DefaultWebSecurityExpressionHandler}
import java.lang.reflect.Method
import java.util.regex.{Matcher, Pattern}

class WebExpressionHandler extends DefaultWebSecurityExpressionHandler {

  override def createEvaluationContextInternal(authentication: Authentication, invocation: FilterInvocation) = {
    new WebExpressionEvalutionContext(authentication, invocation)
  }
}

class WebExpressionEvalutionContext(authentication: Authentication, invocation: FilterInvocation) extends StandardEvaluationContext {
  this.registerFunction("urlId", UrlRegexp.urlID)

  override def lookupVariable(name: String) = {
    Option(super.lookupVariable(name)).getOrElse(
      name match {
        case "url" => invocation.getRequestUrl
        case _ => invocation.getHttpRequest.getParameter(name)
      }
    )
  }
}


object UrlRegexp {
  val thisClazz = Class.forName("com.griddynamics.genesis.spring.security.UrlRegexp")

  val urlID: Method = thisClazz.getDeclaredMethod("urlID", classOf[String], classOf[String])

  /**
   * Note that for this method to work properly provided pattern _SHOULD NOT_ contain regexp groups
   */
  def urlID(pattern: String, url: String): Long = {
    val matcher: Matcher = Pattern.compile(pattern.replaceAll("\\{id\\}", "(\\\\d+)")).matcher(url) //todo(RB): move to named groups after jdk 6 stop being supported by genesis

    if(matcher.find()) {
      matcher.group(1).toInt
    } else {
      throw new IllegalStateException("Url [" + url + "] doesn't match to pattern [" + pattern + "]" )
    }
  }
}
