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

import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.http.{UrlConnectionTunnel, TunnelFilter}
import com.griddynamics.genesis.resources.ResourceFilter
import com.griddynamics.genesis.service.ConfigService
import com.griddynamics.genesis.service.GenesisSystemProperties._
import com.griddynamics.genesis.util.{RichLogger, Logging}
import com.yammer.metrics.jetty.{InstrumentedSslSelectChannelConnector, InstrumentedSelectChannelConnector}
import com.yammer.metrics.reporting.AdminServlet
import com.yammer.metrics.web.DefaultWebappMetricsFilter
import java.lang.System
import java.lang.System.{getProperty => gp}
import java.util.Properties
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.SystemUtils
import org.eclipse.jetty.server.{Connector, Server}
import org.eclipse.jetty.servlet.{FilterHolder, ServletHolder, ServletContextHandler}
import org.eclipse.jetty.servlets.GzipFilter
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy
import org.springframework.web.servlet.DispatcherServlet
import scala.collection.JavaConversions._
import service.impl.HousekeepingService
import collection.mutable.ListBuffer
import org.eclipse.jetty.server.nio.{AbstractNIOConnector, SelectChannelConnector}
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import javax.servlet.DispatcherType
import org.eclipse.jetty.util.ssl.SslContextFactory

object GenesisFrontend extends Logging {
    def main(args: Array[String]): Unit = try {
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
        val profiles = Option(System.getProperty("spring.profiles.active"))

        profiles match {
          case Some(p) if (!p.trim().isEmpty) => System.setProperty("spring.profiles.active", p + ", server")
          case _ => System.setProperty("spring.profiles.active", "server")
        }
        val appContext = new ClassPathXmlApplicationContext(contexts:_*)
        appContext.getEnvironment.setActiveProfiles("server")
        val helper = new PropertyHelper(genesisProperties, appContext)
        helper.validateProperties(log)

        if (!isFrontend) {
          val houseKeepingService = appContext.getBean(classOf[HousekeepingService])
          doHousekeeping(houseKeepingService)
          installShutdownHook(houseKeepingService, helper)
        }

        val resourceRoots = helper.getPropWithFallback(WEB_RESOURCE_ROOTS, "classpath:genesis/")
        val debugMode = helper.getPropWithFallback(CLIENT_DEBUG_MODE, "false")
        val metrics = helper.getPropWithFallback(METRICS, true)
        val metricsAdd = !isFrontend && metrics
        val server = new Server()

        val webAppContext = new GenericWebApplicationContext
        val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
        val servletContext = context.getServletContext
        webAppContext.setServletContext(servletContext)
        webAppContext.setParent(appContext)
        webAppContext.refresh()
        context.setContextPath("/")
        servletContext.setAttribute(ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webAppContext)

        if (metricsAdd) addMetrics(context)

        if (! isFrontend) {
            val gzipFilterHolder = new FilterHolder(new GzipFilter)
            gzipFilterHolder.setName("gzipFilter")
            gzipFilterHolder.setInitParameter("mimeTypes", "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml")
            context.addFilter(gzipFilterHolder, "/*", java.util.EnumSet.of(DispatcherType.REQUEST))
        }

        val securityFilterHolder = new FilterHolder(new DelegatingFilterProxy)
        securityFilterHolder.setName("springSecurityFilterChain")
        context.addFilter(securityFilterHolder, "/*", java.util.EnumSet.of(DispatcherType.REQUEST))
        context.setInitParameter(LOGOUT_ENABLED, logoutEnabled.toString)

        if (! isBackend) {
            val resourceHolder = new FilterHolder(new ResourceFilter)
            resourceHolder.setName("resourceFilter")
            resourceHolder.setInitParameter("resourceRoots", resourceRoots)
            resourceHolder.setInitParameter("debugMode", debugMode)
            context.addFilter(resourceHolder, "/*", java.util.EnumSet.of(DispatcherType.REQUEST))
        }

        if (isFrontend) {
          val filteredUris = ListBuffer("/rest")
          if (metrics) filteredUris += "/metrics"
            val proxyFilter = new TunnelFilter(filteredUris.toArray) with UrlConnectionTunnel
            val proxyHolder = new FilterHolder(proxyFilter)
            proxyHolder.setInitParameter(TunnelFilter.BACKEND_PARAMETER, helper.getFileProperty(SERVICE_BACKEND_URL, ""))
            proxyHolder.setInitParameter(TunnelFilter.READ_TIMEOUT, helper.getFileProperty(FRONTEND_READ_TIMEOUT, "5000"))
            proxyHolder.setInitParameter(TunnelFilter.CONNECT_TIMEOUT, helper.getFileProperty(FRONTEND_CONNECT_TIMEOUT, "5000"))
            proxyHolder.setName("tunnelFilter")
            context.addFilter(proxyHolder, "/*", java.util.EnumSet.of(DispatcherType.REQUEST))
        }

        val holder = new ServletHolder(new DispatcherServlet)
        val frontendConfig: String = if (isFrontend) "classpath:/WEB-INF/spring/proxy-config.xml" else "classpath:/WEB-INF/spring/frontend-config.xml"
        log.debug("Using frontend configuration: %s", frontendConfig)
        holder.setInitParameter("contextConfigLocation", frontendConfig)
        context.addServlet(holder, "/")

        val httpPort = helper.getPropWithFallback(BIND_PORT, 8080)
        val httpsPort = helper.getPropWithFallback(HTTPS_PORT, 8443)
        val useHttps = helper.getPropWithFallback(USE_HTTPS, false)
        val useHttp = helper.getPropWithFallback(USE_HTTP, true)
        val keyStorePath = helper.getPropWithFallback(SSL_KEYSTORE, ".keystore")
        val keyStorePass = helper.getPropWithFallback(SSL_KEYSTORE_PASSWORD, "")

        log.debug(s"Following connectors are enabled with configuration: HTTP: ${useHttp}, HTTPS: ${useHttps}")

        val connectors = (useHttp, useHttps) match {
          case (true, true) => httpConnector(httpPort, metricsAdd) :: httpsConnector(httpsPort, metricsAdd, keyStorePath, keyStorePass) :: Nil
          case (true, _) => httpConnector(httpPort, metricsAdd) :: Nil
          case (_, true) => httpsConnector(httpsPort, metricsAdd, keyStorePath, keyStorePass) :: Nil
          case (_, _) => {
            throw new IllegalArgumentException("You must enable either HTTP or HTTPS or both to run Genesis")
          }
        }

        connectors.map(c => server.addConnector(configureConnector(helper, c)))

        server.setHandler(context)
        server.setStopAtShutdown(true)

        try {
            server.start()
            server.join()
        } finally {
            server.stop()
        }
    } catch {
      case e: Throwable => {
        log.error(e, e.getMessage)
        throw e
      }
    }


  def httpConnector(httpPort: Int, metricsAdd: Boolean) = {
    if (metricsAdd) new InstrumentedSelectChannelConnector(httpPort)
    else {
      val connector = new SelectChannelConnector()
      connector.setPort(httpPort)
      connector
    }
  }

  def httpsConnector(httpPort: Int, metricsAdd: Boolean, keyStorePath: String, keyStorePass: String) = {
    val factory = new SslContextFactory(keyStorePath)
    factory.setKeyStorePassword(keyStorePass)
    if (metricsAdd) new InstrumentedSslSelectChannelConnector(factory, httpPort)
    else {
      val connector = new SslSelectChannelConnector(factory)
      connector.setPort(httpPort)
      connector
    }
  }

  def configureConnector(helper: PropertyHelper, httpConnector: AbstractNIOConnector) : Connector = {
    val host = helper.getPropWithFallback(BIND_HOST, "0.0.0.0")
    val requestIdleTime: Int = helper.getFileProperty(MAX_IDLE, 5000)

    httpConnector.setMaxIdleTime(requestIdleTime)
    httpConnector.setHost(host)
    httpConnector.setRequestHeaderSize(16 * 1024) // authorization negotiate can contain lots of data
    httpConnector.setResponseHeaderSize(16 * 1024) // http://serverfault.com/questions/362280/apache-bad-request-size-of-a-request-header-field-exceeds-server-limit-with-ke

    if (SystemUtils.IS_OS_WINDOWS)
      httpConnector.setReuseAddress(false)
    httpConnector
  }

  def addMetrics(context: ServletContextHandler) {
    val metricsFilter = new DefaultWebappMetricsFilter()
    context.addFilter(new FilterHolder(metricsFilter), "/*", java.util.EnumSet.of(DispatcherType.REQUEST))

    val adminServlet = new AdminServlet()
    context.addServlet(new ServletHolder(adminServlet), "/metrics/*")
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
      houseKeeping.markExecutingWorkflowsAsFailed()
    } catch {
      case e: Exception => log.error("Failed to complete housekeeping", e)
    }
  }

  def loadGenesisProperties(): scala.collection.Map[String, String]  = {
    val resourceLoader = new DefaultResourceLoader()

    val pathToProperties = Option(gp(BACKEND))
      .getOrElse(throw new RuntimeException("Required system property '%s' is undefined".format(BACKEND)))

    val propertiesStream = resourceLoader.getResource(pathToProperties).getInputStream

    val genesisProperties = new Properties()
    genesisProperties.load(propertiesStream)

    propertiesStream.close()
    genesisProperties
  }

}

private class PropertyHelper(genesisProperties: scala.collection.Map[String, String], appContext: ClassPathXmlApplicationContext) {

  private val configServiceOpt = appContext.getBeansOfType(classOf[ConfigService]).headOption.map(_._2)

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

  def getPropWithFallback[T](name: String, default: T) = configServiceOpt.map(_.get(name, default)).getOrElse(
    getFileProperty(name, default)
  )

  def validateProperties(log: RichLogger) = configServiceOpt map (
    configService => configService.validateSettings match {
      case f: Failure => log.error("Settings validation failed!!!")
      val failedPropNames = f.variablesErrors.keySet
      (f.variablesErrors ++ f.serviceErrors).foreach(
        e => log.error("Validation of Genesis configuration property [%s] failed: %s", e._1, e._2)
      )
      (f.compoundVariablesErrors ++ f.compoundServiceErrors).foreach(log.error("%s", _))
      failedPropNames.filter(configService.isImportant(_)) match {
        case s if s.nonEmpty =>
          throw new RuntimeException("Failed to start Genesis, following settings are invalid: %s".format(s.mkString(" ")))
        case _ => log.warn("There are property validation errors, but no essential properties are affected.")
      }

      case _ => log.debug("Settings validation passed.")
    }) getOrElse // TODO: validate file properties
    log.debug("Skipping settings validation, probably running in front-end mode!")

}