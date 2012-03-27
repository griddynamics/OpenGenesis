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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.spring.security

import org.springframework.security.web.AuthenticationEntryPoint
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.security.core.AuthenticationException
import com.griddynamics.genesis.rest.GenesisRestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

class GenesisAuthenticationEntryPoint(val realmName : String, val loginUrl: String) extends LoginUrlAuthenticationEntryPoint(loginUrl)  {

    /**
     * Commences an authentication scheme.
     * <p>
     * <code>ExceptionTranslationFilter</code> will populate the <code>HttpSession</code> attribute named
     * <code>AbstractAuthenticationProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY</code> with the requested target URL before
     * calling this method.
     * <p>
     * Implementations should modify the headers on the <code>ServletResponse</code> as necessary to
     * commence the authentication process.
     *
     * @param request that resulted in an <code>AuthenticationException</code>
     * @param response so that the user agent can begin authentication
     * @param authException that caused the invocation
     *
     */
    override def commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        Option(classOf[GenesisRestController].getAnnotation(classOf[RequestMapping])) match {
            case Some(annotationValue : RequestMapping) => {
                val pattern = request.getContextPath + annotationValue.value().head
                if (request.getRequestURI.startsWith(pattern)) {
                    response.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"")
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage)
                }
                else
                    super.commence(request, response, authException)
            }
            case _ => super.commence(request, response, authException)
        }
    }

}