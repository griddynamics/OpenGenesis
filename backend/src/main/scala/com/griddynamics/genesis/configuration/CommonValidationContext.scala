/*
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

package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Bean, Configuration}
import com.griddynamics.genesis.validation._
import org.springframework.beans.factory.annotation.Autowired

@Configuration
class CommonValidationContext {
  private val REGEX_HOST = "^(?:(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|(?:(([a-zA-Z]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))$"
  @Autowired var validator: javax.validation.Validator = _

  @Bean(name = Array("required")) def requiredConfigValidator = new NotEmptyValidator
  @Bean(name = Array("host")) def hostNameConfigValidator = new RegexValidator(Option(REGEX_HOST))
  @Bean(name = Array("port")) def portConfigValidator = new IntValidator(min = 1, max = 32767)
  @Bean(name = Array("int_nonnegative")) def intNonNegativeConfigValidator = new IntValidator(min = 0)
  @Bean(name = Array("int_positive")) def intPositiveConfigValidator = new IntValidator(min = 1)
  @Bean(name = Array("email")) def emailConfigValidator = new EmailConfigValidator(validator)
  @Bean(name = Array("url")) def urlConfigValidator = new UrlConfigValidator(validator)
  @Bean(name = Array("default_length")) def defaultLengthConfigValidator = new LengthConfigValidator
}
