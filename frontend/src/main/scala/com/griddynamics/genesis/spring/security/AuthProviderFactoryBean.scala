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

import org.springframework.security.authentication.AuthenticationProvider
import com.griddynamics.genesis.service.ConfigService
import com.griddynamics.genesis.service.GenesisSystemProperties.AUTH_MODE
import org.springframework.beans.factory.FactoryBean
import com.griddynamics.genesis.util.Logging
import reflect.BeanProperty
import java.util.{List => JList}
import scala.collection.JavaConversions._
import com.griddynamics.genesis.spring.ApplicationContextAware


class AuthProviderFactoryBean extends FactoryBean[AuthenticationProvider] with ApplicationContextAware with Logging {

  import AuthProviderFactoryBean._

  @BeanProperty var configService: ConfigService = _
  @BeanProperty var defaultAuthProvider: AuthenticationProvider = _

  def getObject = {
    val mode = configService.get(AUTH_MODE, DEFAULT)

    val providerFactories = applicationContext.getBeansOfType(classOf[AuthProviderFactory]).values()

    mode match {
      case DEFAULT => defaultAuthProvider
      case _ => providerFactories.toSeq.find(_.mode == mode) match {
        case Some(factory) => {
          if (factory.capable()) {
            factory.create()
          } else {
            log.warn("Authentication provider for selected mode '%s' is broken. Default auth schema will be used", mode)
            defaultAuthProvider
          }
        }
        case None => {
          log.warn("Unsupported auth mode '%s'. Default auth schema will be used", mode)
          defaultAuthProvider
        }
      }
    }
  }

  def getObjectType = classOf[AuthenticationProvider]

  def isSingleton = false
}

object AuthProviderFactoryBean {
  val DEFAULT = "default"
}