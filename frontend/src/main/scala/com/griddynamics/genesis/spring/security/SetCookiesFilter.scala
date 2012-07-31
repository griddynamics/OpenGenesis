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

import org.springframework.web.filter.GenericFilterBean
import javax.servlet.{FilterChain, ServletResponse, ServletRequest}
import org.springframework.security.core.context.SecurityContextHolder
import javax.servlet.http.{Cookie, HttpServletResponse, HttpServletRequest}
import org.springframework.security.core.Authentication
import java.security.Principal

class SetCookiesFilter extends GenericFilterBean {
  def doFilter(p1: ServletRequest, p2: ServletResponse, p3: FilterChain) {
    val request = p1.asInstanceOf[HttpServletRequest]
    val response = p2.asInstanceOf[HttpServletResponse]
    val find: Option[Cookie] = request.getCookies match{
      case null => None
      case _ => request.getCookies.find(_.getName == "GENESIS_USERNAME")
    }
    find match {
      case None => {
        val authentication: Authentication = SecurityContextHolder.getContext.getAuthentication
        if (authentication != null && authentication.isInstanceOf[Principal])
          response.addCookie(new Cookie("GENESIS_USERNAME", authentication.asInstanceOf[Principal].getName))
      }
      case _ =>
    }
    
    p3.doFilter(p1, p2)
  }
}
