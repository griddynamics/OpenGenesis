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

import org.springframework.security.web.authentication.{AuthenticationFailureHandler => SpringAuthenticationFailureHandler, AuthenticationSuccessHandler => SpringAuthenticationSuccessHandler}
import java.io.Writer
import org.springframework.security.core.{AuthenticationException, Authentication}
import com.griddynamics.genesis.spring.security.Utils._
import javax.servlet.http.{Cookie, HttpServletResponseWrapper, HttpServletResponse, HttpServletRequest}
import org.springframework.security.core.userdetails.UserDetails

class AuthenticationSuccessHandler extends SpringAuthenticationSuccessHandler {

    def onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse, auth: Authentication) {
      createSessionIfNotExist(request)
      writeResponse(response, "{\"success\": true}")
    }

    private def createSessionIfNotExist(request: HttpServletRequest) {
      request.getSession()
    }
}

class SetGenesisCookieHandler() extends SpringAuthenticationSuccessHandler {
  def onAuthenticationSuccess(p1: HttpServletRequest, p2: HttpServletResponse, p3: Authentication) {
    val principal: AnyRef = p3.getPrincipal
    p2.addCookie(new Cookie("GENESIS_USERNAME", principal.asInstanceOf[UserDetails].getUsername))
  }
}

class AuthenticationFailureHandler extends SpringAuthenticationFailureHandler {
    def onAuthenticationFailure(request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException) {
        writeResponse(response, "{\"success\": false, \"errors\": \"Login failed. Try again.\"}")
    }
}

object Utils{
    def writeResponse(response: HttpServletResponse, result: String){
        val responseWrapper: HttpServletResponseWrapper = new HttpServletResponseWrapper(response)
        val out: Writer = responseWrapper.getWriter
        try{
            out.write(result)
        } finally {
            out.close()
        }
    }
}