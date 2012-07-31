/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.spring.security

import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.core.AuthenticationException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.http.MediaType
import com.griddynamics.genesis.util.Closeables

class GenesisAuthenticationEntryPoint(val loginUrl: String) extends LoginUrlAuthenticationEntryPoint(loginUrl) {
  override def commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
    val acceptMediaTypes = MediaType.parseMediaTypes(request.getHeader("Accept"))

    if(acceptMediaTypes.contains(MediaType.APPLICATION_JSON)){
      response.setContentType(MediaType.APPLICATION_JSON.toString)
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
      Closeables.using(response.getOutputStream){
        ostream => ostream.write(("{\"error\":\"" + authException.getMessage + "\"}").getBytes)
      }
    } else {
      super.commence(request, response, authException)
    }
  }
}