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
package com.griddynamics.genesis

import http.{UrlConnectionTunnel, TunnelFilter}
import org.springframework.core.io.DefaultResourceLoader
import java.util.Properties
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.filter.DelegatingFilterProxy
import org.eclipse.jetty.servlet.{FilterHolder, ServletHolder, ServletContextHandler}
import org.eclipse.jetty.servlets.GzipFilter
import org.apache.commons.lang3.SystemUtils
import java.lang.System.{getProperty => gp}
import resources.ResourceFilter
import service.ConfigService
import service.GenesisSystemProperties._
import service.impl.HousekeepingService
import com.griddynamics.genesis.util.Logging
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
import java.util.concurrent.TimeUnit
import java.lang.System
import scala.collection.JavaConversions._

object GenesisFrontend extends Logging {
    def main(args: Array[String]): Unit = try{
        val genesisProperties = loadGenesisProperties()

        val securityConfig = genesisProperties.getOrElse(SECURITY_CONFIG, "classpath:/WEB-INF/spring/security-config.xml")
        val logoutEnabled = genesisProperties.getOrElse(LOGOUT_ENABLED, true)
        val isFrontend = genesisProperties.get(SERVICE_BACKEND_URL).isDefined
        val isBackend = genesisProperties.getOrElse(SERVER_MODE, "frontend") == "backend"

        val contexts = if (isFrontend)
          Seq(securityConfig)
         else
          Seq("classpath:/WEB-INF/spring/backend-config.xml", securityConfig)

        log.debug("Using contexts %s", contexts)

        val appContext = new ClassPathXmlApplicationContext(contexts:_*)
        val helper = new PropertyHelper(genesisProperties, appContext)

        val requestIdleTime: Int = helper.getFileProperty(MAX_IDLE, 5000)

        if (!isFrontend) {
          val houseKeepingService = appContext.getBean(classOf[HousekeepingService])
          doHousekeeping(houseKeepingService)
          installShutdownHook(houseKeepingService, helper)
        }

        val host = helper.getPropWithFallback(BIND_HOST, "0.0.0.0")
        val port = helper.getPropWithFallback(BIND_PORT, 8080)
        val resourceRoots = helper.getPropWithFallback(WEB_RESOURCE_ROOTS, "classpath:")
        val cacheResources = helper.getPropWithFallback(CACHE_RESOURCES, "true")
        val server = new Server()

        val webAppContext = new GenericWebApplicationContext
        val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
        val servletContext = context.getServletContext
        webAppContext.setServletContext(servletContext)
        webAppContext.setParent(appContext)
        webAppContext.refresh()
        context.setContextPath("/")
        servletContext.setAttribute(ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webAppContext)

        if (! isFrontend) {
            val gzipFilterHolder = new FilterHolder(new GzipFilter)
            gzipFilterHolder.setName("gzipFilter")
            gzipFilterHolder.setInitParameter("mimeTypes", "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml")
            context.addFilter(gzipFilterHolder, "/*", 0)
        }

        val securityFilterHolder = new FilterHolder(new DelegatingFilterProxy)
        securityFilterHolder.setName("springSecurityFilterChain")
        context.addFilter(securityFilterHolder, "/*", 0)
        context.setInitParameter(LOGOUT_ENABLED, logoutEnabled.toString)

        if (! isBackend) {
            val resourceHolder = new FilterHolder(new ResourceFilter)
            resourceHolder.setName("resourceFilter")
            resourceHolder.setInitParameter("resourceRoots", resourceRoots)
            resourceHolder.setInitParameter("cacheResources", cacheResources)
            context.addFilter(resourceHolder, "/*", 0)
        }

        if (isFrontend) {
            val proxyFilter = new TunnelFilter("/rest") with UrlConnectionTunnel
            val proxyHolder = new FilterHolder(proxyFilter)
            proxyHolder.setInitParameter(TunnelFilter.BACKEND_PARAMETER, helper.getFileProperty(SERVICE_BACKEND_URL, ""))
            proxyHolder.setInitParameter(TunnelFilter.READ_TIMEOUT, helper.getFileProperty(FRONTEND_READ_TIMEOUT, "5000"))
            proxyHolder.setInitParameter(TunnelFilter.CONNECT_TIMEOUT, helper.getFileProperty(FRONTEND_CONNECT_TIMEOUT, "5000"))
            proxyHolder.setName("tunnelFilter")
            context.addFilter(proxyHolder, "/*", 0)
        }

        val holder = new ServletHolder(new DispatcherServlet)
        val frontendConfig: String = if (isFrontend) "classpath:/WEB-INF/spring/proxy-config.xml" else "classpath:/WEB-INF/spring/frontend-config.xml"
        log.debug("Using frontend configuration: %s", frontendConfig)
        holder.setInitParameter("contextConfigLocation", frontendConfig)
        context.addServlet(holder, "/")
        val httpConnector = new SelectChannelConnector()
        httpConnector.setMaxIdleTime(requestIdleTime)
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
    } catch {
      case e => {
        log.error(e, e.getMessage)
        throw e
      }
    }


  def installShutdownHook(houseKeepingService: HousekeepingService, helper: PropertyHelper) {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      val WaitingPeriod = 2000

      override def run() {
        val timeout = TimeUnit.SECONDS.toMillis(helper.getPropWithFallback(SHUTDOWN_TIMEOUT, 60))
        val shutdownStart = System.currentTimeMillis()

        var envs = houseKeepingService.allEnvsWithActiveWorkflows
        if (!envs.isEmpty) {
          houseKeepingService.cancelAllWorkflows(envs)
        }

        while (!envs.isEmpty && (timeout == 0 || System.currentTimeMillis() - shutdownStart < timeout)) {
          log.info("Terminating running workflows. Active environments: " + envs.size)
          Thread.sleep(WaitingPeriod)
          envs = houseKeepingService.allEnvsWithActiveWorkflows
        }

        if (envs.isEmpty) {
          log.info("Workflows termination process finished sucessfully")
        } else {
          log.warn("Workflow temination timed out. Active environments: " + envs.size)
        }
      }
    })
  }

  def doHousekeeping(houseKeeping: HousekeepingService) {
    log.info("Housekeeping: marking executing workflows statuses as failed")
    try {
      houseKeeping.markExecutingWorkflowsAsFailed();
    } catch {
      case e => log.error("Failed to complete housekeeping", e)
    }
  }

  def loadGenesisProperties(): scala.collection.Map[String, String]  = {
    val resourceLoader = new DefaultResourceLoader()
    val propertiesStream = resourceLoader.getResource(gp(BACKEND)).getInputStream

    val genesisProperties = new Properties()
    genesisProperties.load(propertiesStream)

    propertiesStream.close()
    genesisProperties
  }

}


private class PropertyHelper(genesisProperties: scala.collection.Map[String, String], appContext: ClassPathXmlApplicationContext) {

  def getFileProperty[T](name: String, default: T) = {
    val strVal = gp(name, genesisProperties.getOrElse(name, String.valueOf(default)))
    (default match {
      case v: Int => strVal.toInt
      case v: Long => strVal.toLong
      case v: String => strVal
      case v: Boolean => strVal.toBoolean
      case _ => throw new IllegalArgumentException("Not supported type")
    }).asInstanceOf[T]
  }

  def getProperty[T](name: String, default: T) = appContext.getBean(classOf[ConfigService]).get(name, default)

  def getPropWithFallback[T](name: String, default: T) = {
    appContext.getBeansOfType(classOf[ConfigService]).isEmpty match {
      case true => getFileProperty(name, default)
      case false => getProperty(name, default)
    }
  }
}