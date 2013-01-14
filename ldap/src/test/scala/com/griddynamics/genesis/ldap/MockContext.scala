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
package com.griddynamics.genesis.ldap

import org.springframework.context.annotation.{Bean, Configuration}
import java.util.Properties
import org.springframework.core.io.ClassPathResource
import com.griddynamics.genesis.service.{ProjectAuthorityService, AuthorityService, ConfigService}
import org.mockito.Matchers
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.mockito.Mockito._
import com.griddynamics.genesis.api.UserGroup
import com.griddynamics.genesis.users.GenesisRole

@Configuration
class MockContext {

  val properties = {
    val props = new Properties()
    props.load(new ClassPathResource("plugin.properties").getInputStream)
    props
  }

  @Bean def configService: ConfigService = {
    val service = mock(classOf[ConfigService])

    when(service.get(Matchers.any(classOf[String]))).thenAnswer(new Answer[Option[Any]] {
      def answer(invocation: InvocationOnMock) =
        Option(properties.getProperty(invocation.getArguments.toSeq(0).asInstanceOf[String]))
    })

    when(service.get(Matchers.any(classOf[String]), Matchers.any())).thenAnswer(new Answer[Any] {
      def answer(invocation: InvocationOnMock) =
        Option(properties.getProperty(invocation.getArguments.toSeq(0).asInstanceOf[String]))
          .getOrElse(invocation.getArguments.toSeq(1))
    })

    service
  }

  @Bean def authorityService: AuthorityService = {
    val service = mock(classOf[AuthorityService])

    when(service.getAuthorities(Matchers.any())).thenAnswer(new Answer[List[String]] {
      def answer(invocation: InvocationOnMock) = {
        val groups = invocation.getArguments.toSeq(0).asInstanceOf[Iterable[UserGroup]]
        groups.toList flatMap (_.name match {
          case "managers" => List(GenesisRole.ReadonlySystemAdmin.toString)
          case _ => List.empty
        })
      }
    })

    when(service.getUserAuthorities(Matchers.eq("developer1")))
      .thenReturn(List(GenesisRole.SystemAdmin.toString))
    when(service.getUserAuthorities(Matchers.eq("developer2")))
      .thenReturn(List.empty)
    when(service.getUserAuthorities(Matchers.eq("manager1")))
      .thenReturn(List.empty)

    service
  }

  @Bean def projectAuthorityService: ProjectAuthorityService = mock(classOf[ProjectAuthorityService])

}
