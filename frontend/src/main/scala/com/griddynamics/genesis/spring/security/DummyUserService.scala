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

import java.lang.String
import org.springframework.security.core.userdetails.{UserDetails, UserDetailsService}
import org.springframework.security.core.GrantedAuthority
import java.util.Arrays

/*
 * Copyright (c) 2011 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   For information about the licensing and copyright of this document please
 *   contact Grid Dynamics at info@griddynamics.com.
 *
 *   $Id: $
 *   @Project:     Genesis
 *   @Description: A cloud deployment platform
 */
// TODO: will be replaced by real user service in the meantime.
// While on windows genesis will use waffle, we don't need to mess with AD right now
class DummyUserService extends UserDetailsService{
  def loadUserByUsername(p1: String) = new UserDetails(){
    def getAuthorities = Arrays.asList(new GrantedAuthority {
      def getAuthority = "ROLE_GENESIS"
    });

    def getPassword = ""

    def getUsername = p1

    def isAccountNonExpired = true

    def isAccountNonLocked = true

    def isCredentialsNonExpired = true

    def isEnabled = true
  }
}