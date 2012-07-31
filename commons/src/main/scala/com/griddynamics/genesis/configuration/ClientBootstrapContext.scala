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

import org.springframework.context.annotation.{Scope, Bean, Configuration}
import org.springframework.beans.factory.config.BeanDefinition
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{Channels, ChannelPipelineFactory}
import org.jboss.netty.handler.logging.LoggingHandler
import org.jboss.netty.logging.{InternalLogLevel, Slf4JLoggerFactory, InternalLoggerFactory}
import org.jboss.netty.util.HashedWheelTimer
import org.jboss.netty.handler.timeout.{ReadTimeoutHandler, IdleStateHandler}
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.{Executors, TimeUnit}


@Configuration
class ClientBootstrapContext {

    @Bean
    @Scope(value = BeanDefinition.SCOPE_PROTOTYPE) def clientBootstrap = {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        val bootstrap = new ClientBootstrap(socketChannelFactory)
        bootstrap.setPipelineFactory(channelPipelineFactory)
        bootstrap.setOption("connectTimeoutMillis", 10000)
        bootstrap
    }

    def channelPipelineFactory = {
        new ChannelPipelineFactory() {
            def getPipeline = {
                Channels.pipeline(new LoggingHandler(InternalLogLevel.INFO));
                Channels.pipeline(new IdleStateHandler(hashedWheelTimer, 10, 10, 10))
                Channels.pipeline(new ReadTimeoutHandler(hashedWheelTimer, 10, TimeUnit.SECONDS))
            }
        }
    }


  @Bean def hashedWheelTimer: HashedWheelTimer = new HashedWheelTimer()

  def socketChannelFactory = {
        new NioClientSocketChannelFactory(
            Executors.newSingleThreadExecutor(),
            Executors.newSingleThreadExecutor()
        )
    }
}
