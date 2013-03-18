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
 */ package com.griddynamics.genesis.scheduler

import com.griddynamics.genesis.service.ExecutedJobsService
import com.griddynamics.genesis.repository.impl.FailedJobRepository
import com.griddynamics.genesis.api.{ExtendedResult, ScheduledJobDetails, ScheduledJobStat, Success, Failure}
import org.springframework.transaction.annotation.Transactional

class ExecutedJobsServiceImpl(failedJobsRepo: FailedJobRepository, envJobService: EnvironmentJobService) extends ExecutedJobsService {

  @Transactional(readOnly = true)
  def listFailedJobs(envId: Int) = failedJobsRepo.list(envId)

  @Transactional
  def removeFailedJobRecord(envId: Int, jobId: Int) = {
    failedJobsRepo.delete(envId, jobId) match {
      case 0 => Failure(compoundServiceErrors = Seq(s"Failed to find jobId $jobId related to env id = $envId"), isNotFound = true)
      case _ => Success(jobId)
    }
  }

  @Transactional
  def removeFailedJobsRecords(projectId: Int, envId: Int): ExtendedResult[Int] = {
    Success(failedJobsRepo.deleteRecords(projectId, envId))
  }

  @Transactional(readOnly = true)
  def jobsStat: Seq[ScheduledJobStat] = {
    val scheduled = envJobService.scheduledJobsStat
    val failed = failedJobsRepo.failedJobStats.toMap
    (failed.map { case (projectId, failedCount) =>
      new ScheduledJobStat(projectId = projectId, scheduledJobs = scheduled.getOrElse(projectId, 0), failedJobs = failedCount)
    } ++ (scheduled -- failed.keys).map { case (projectId, requested) =>
      new ScheduledJobStat(projectId = projectId, scheduledJobs = requested, failedJobs = 0)
    }).toSeq
  }

  @Transactional(readOnly = true)
  def listProjectJobs(projectId: Int): (Seq[ScheduledJobDetails], Seq[ScheduledJobDetails]) = {
    (failedJobsRepo.listByProject(projectId).toSeq, envJobService.listScheduledJobs(projectId))
  }
}
