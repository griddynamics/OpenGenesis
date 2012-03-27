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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis

import org.springframework.core.io.DefaultResourceLoader
import java.util.Properties
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.filter.DelegatingFilterProxy
import org.eclipse.jetty.servlet.{FilterHolder, ServletHolder, ServletContextHandler}
import org.eclipse.jetty.servlets.GzipFilter
import org.apache.commons.lang3.SystemUtils
import java.lang.System.{getProperty => gp}
import resources.ResourceFilter
import spring.security.DefaultSecurityConfig
import util.Logging

object GenesisFrontend extends Logging with DefaultSecurityConfig {
    val genesisProperties = loadGenesisProperties()

    def main(args: Array[String]) {
        val host = getProperty("genesis.bind.host", "0.0.0.0")
        val port = Integer.valueOf(getProperty("genesis.bind.port", "8080"))
        val isFrontend = getProperty("genesis.service.backendUrl", "NONE") != "NONE"
        val resourceRoots = getProperty("genesis.web.resourceRoots", "classpath:,classpath:resources/,classpath:resources/icons/,classpath:extjs/")
        val securityConfig: String = securityConfigResolver.findSecurityConfig(genesisProperties)
        log.debug("Using configuration file %s", securityConfig)
        val server = new Server()

        val context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        val gzipFilterHolder = new FilterHolder(new GzipFilter)
        gzipFilterHolder.setName("gzipFilter")
        gzipFilterHolder.setInitParameter("mimeTypes", "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml")
        context.addFilter(gzipFilterHolder, "/*", 0)

        val securityFilterHolder = new FilterHolder(new DelegatingFilterProxy)
        securityFilterHolder.setName("springSecurityFilterChain")
        context.addFilter(securityFilterHolder, "/*", 0)

        context.addEventListener(new ContextLoaderListener)
        val contexts = if (isFrontend) {
            securityConfig
        } else {
            "classpath:/WEB-INF/spring/backend-config.xml " + securityConfig
        }
        log.debug("Using contexts %s", contexts)
        context.setInitParameter("contextConfigLocation", contexts)

        val resourceHolder = new FilterHolder(new ResourceFilter)
        resourceHolder.setName("resourceFilter")
        resourceHolder.setInitParameter("resourceRoots", resourceRoots)
        context.addFilter(resourceHolder, "/*", 0)

        val holder = new ServletHolder(new DispatcherServlet)
        val frontendConfig: String = if (isFrontend) "classpath:/WEB-INF/spring/proxy-config.xml" else "classpath:/WEB-INF/spring/frontend-config.xml"
        log.debug("Using frontend configuration: %s", frontendConfig)
        holder.setInitParameter("contextConfigLocation", frontendConfig)
        context.addServlet(holder, "/");

        val httpConnector = new SelectChannelConnector()

        httpConnector.setHost(host)
        httpConnector.setPort(port)

        if (SystemUtils.IS_OS_WINDOWS)
            httpConnector.setReuseAddress(false)

        server.addConnector(httpConnector)
        server.setHandler(context)
        server.setStopAtShutdown(true)

        try {
            server.start()
            server.join()
        } finally {
            server.stop()
        }
    }

    def loadGenesisProperties() = {
        val resourceLoader = new DefaultResourceLoader()
        val propertiesStream = resourceLoader.getResource(
            gp("backend.properties")
        ).getInputStream

        val genesisProperties = new Properties()
        genesisProperties.load(propertiesStream)

        propertiesStream.close()
        genesisProperties
    }

    def getProperty(name: String, default: String) =
        gp(name, genesisProperties.getProperty(name, default))
}
