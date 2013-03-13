/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.configuration

import org.springframework.context.annotation.{Profile, Bean, Configuration}
import com.griddynamics.genesis.scheduler.{ExecutedJobsServiceImpl, FailedJobsListener, SchedulingServiceImpl, EnvironmentJobServiceImpl, NotificationService, SchedulingService, EnvironmentJobService}
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.beans.factory.annotation.{Value, Autowired}
import javax.sql.DataSource
import org.quartz.spi.JobFactory
import com.griddynamics.genesis.scheduler.jobs.WorkflowJobFactory
import com.griddynamics.genesis.bean.RequestBroker
import com.griddynamics.genesis.service.{ExecutedJobsService, EnvironmentConfigurationService, ProjectService}
import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.util.Logging
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import java.util.{Date, Properties}
import javax.annotation.PostConstruct
import com.griddynamics.genesis.model.Environment
import com.griddynamics.genesis.api.Success
import com.griddynamics.genesis.core.events.{EnvDestroyed, WorkflowEventsBus}
import com.griddynamics.genesis.repository.impl.{FailedJobRepositoryImpl, FailedJobRepository}


@Configuration
@Profile(Array("server"))
class SchedulerContext extends Logging {

  @Autowired var dataSource: DataSource = _
  @Autowired var transactionManager: DataSourceTransactionManager = _

  @Autowired var requestBroker: RequestBroker = _
  @Autowired var storeService: StoreServiceContext = _
  @Autowired var templateService: TemplateServiceContext = _
  @Autowired var projectService: ProjectService = _
  @Autowired var userService: UserService = _
  @Autowired var envConfigService: EnvironmentConfigurationService = _

  @Value("${genesis.web.admin.username:NOT-SET}") var adminUserName: String = _
  @Value("${genesis.web.admin.email:NOT-SET}") var adminEmail: String = _

  @PostConstruct
  def afterPropertiesSet() {
    if (adminUserName != "NOT-SET" && adminEmail == "NOT-SET") {
      log.warn(s"Property 'genesis.web.admin.email' is not set. This means that admin '$adminUserName' won't be able to receive email notifications")
    }

    WorkflowEventsBus.subscribe {
      case EnvDestroyed(projectId, envId) => envJobService.removeAllScheduledJobs(projectId, envId)
    }
  }

  @Bean def failedJobRepository: FailedJobRepository = new FailedJobRepositoryImpl

  @Bean def jobFactory: JobFactory = new WorkflowJobFactory(requestBroker, storeService.storeService, notificationService)

  @Bean def envJobService: EnvironmentJobService = new EnvironmentJobServiceImpl (
    schedulingService,
    storeService.storeService,
    templateService.templateService,
    envConfigService
  )

  @Bean def schedulingService: SchedulingService = new SchedulingServiceImpl(scheduler.getObject)

  @Bean def notificationService: NotificationService = new NotificationService(
    adminUsername = if(adminUserName == "NOT-SET") None else Some(adminUserName),
    adminEmail = if (adminEmail == "NOT-SET") None else Some(adminEmail),
    userService = userService,
    projectService = projectService )

  @Bean def jobListener = new FailedJobsListener(failedJobRepository)

  @Bean def executedJobsService: ExecutedJobsService = new ExecutedJobsServiceImpl(failedJobRepository)

  @Bean def scheduler: SchedulerFactoryBean = {
    val factory = new SchedulerFactoryBean()
    factory.setDataSource(dataSource)
    factory.setTransactionManager(transactionManager)
    factory.setJobFactory(jobFactory)

    factory.setQuartzProperties( Map (
      "org.quartz.jobStore.class" -> "org.quartz.impl.jdbcjobstore.JobStoreTX",
      "org.quartz.jobStore.misfireThreshold" -> "60000",
      "org.quartz.plugin.triggHistory.class" -> "org.quartz.plugins.history.LoggingTriggerHistoryPlugin",
      "org.quartz.plugin.triggHistory.triggerFiredMessage" -> "Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss dd/MM/yyyy}",
      "org.quartz.plugin.triggHistory.triggerCompleteMessage" -> "Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss dd/MM/yyyy} with resulting trigger instruction code: {9}",
      "org.quartz.plugin.jobHistory.class" -> "org.quartz.plugins.history.LoggingJobHistoryPlugin",
      "org.quartz.plugin.jobHistory.jobSuccessMessage" -> "Job {1}.{0} fired at: {2, date, dd/MM/yyyy HH:mm:ss} result=OK",
      "org.quartz.plugin.jobHistory.jobFailedMessage" -> "Job {1}.{0} fired at: {2, date, dd/MM/yyyy HH:mm:ss} result=ERROR",
      "org.quartz.jobStore.tablePrefix" -> "QRTZ_",
      "org.terracotta.quartz.skipUpdateCheck" -> "true"
    ))
    factory.setOverwriteExistingJobs(true)
    factory.setStartupDelay(50)
    factory.setApplicationContextSchedulerContextKey("applicationContext")
    factory.setGlobalJobListeners(Array(jobListener))
    factory
  }

  private implicit def mapToProperties(map: Map[String, String]): java.util.Properties = {
    val props = new Properties()
    map.foreach { case (key, value) => props.setProperty(key, value) }
    props
  }
}



@Configuration
@Profile(Array("genesis-cli"))
class SchedulerStubContext {
  @Bean def envJobService: EnvironmentJobService = new EnvironmentJobService {

    def removeScheduledDestruction(projectId: Int, envId: Int) {}

    def executionDate(env: Environment, destroyWorfklow: String) = None

    def destructionDate(env: Environment) = None

    def scheduleDestruction(projectId: Int, envId: Int, date: Date, requestedBy: String) = Success(date)

    def removeAllScheduledJobs(projectId: Int, envId: Int) {}

    def listScheduledJobs(projectId: Int, envId: Int) = Seq()

    def removeJob(projectId: Int, envId: Int, jobId: String) {}

    def scheduleExecution(projectId: Int, envId: Int, workflow: String, parameters: Map[String, String], date: Date, requestedBy: String) = Success(date)
  }
}
