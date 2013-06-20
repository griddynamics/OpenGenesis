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
package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.validation.Validation
import org.springframework.transaction.annotation.Transactional
import com.griddynamics.genesis.validation.Validation._
import com.griddynamics.genesis.api.{Configuration, Success, Project, Ordering}
import com.griddynamics.genesis.service
import com.griddynamics.genesis.model.EnvStatus
import com.griddynamics.genesis.repository.{ConfigurationRepository, ProjectRepository}
import com.griddynamics.genesis.users.GenesisRole
import com.griddynamics.genesis.service.ProjectService
import com.griddynamics.genesis.cache.{CacheManager, Cache}
import com.griddynamics.genesis.util.Logging


class ProjectServiceImpl(
    repository: ProjectRepository,
    storeService: service.StoreService,
    authorityService: service.ProjectAuthorityService,
    configurationRepository: ConfigurationRepository,
    val cacheManager: CacheManager) extends ProjectService
  with Validation[Project] with Cache with Logging {

  override val defaultTtl = 3600
  private val CACHE_REGION = "projects"

  private def getFromCache[B](key: AnyRef)(callback: => B) = fromCache(CACHE_REGION, key)(callback)

  protected def validateCreation(project: Project) =
      must(project, "Project with name '" + project.name + "' already exists") {
        project => findByName(project.name).isEmpty
      }

  protected def validateUpdate(project: Project) =
      must(project, "Project with name '" + project.name + "' already exists") {
        project => repository.findByName(project.name).forall { _.id == project.id}
      } ++
      must(project, "Deleted project can not have active instances") { project =>
        project.isDeleted != true || storeService.countEnvs(project.id.get, EnvStatus.active) == 0
      }

  @Transactional(readOnly = true)
  def get(key: Int): Option[Project] = getFromCache(key.toString) { repository.get(key) }

  @Transactional(readOnly = true)
  def list: Seq[Project] =  fromCache(CACHE_REGION, "*") { repository.list }

  @Transactional(readOnly = true)
  def orderedList(ordering: Ordering) = getFromCache(s"*~${ordering.field}~${ordering.direction}") { repository.list(ordering) }

  @Transactional
  override def create(project: Project) = withEvict(CACHE_REGION) {
    val result = validCreate(project, repository.save(_))

    result.map { pr =>
      configurationRepository.save(new Configuration(None, "Default", pr.id.get, None))
      pr
    }
  }

  @Transactional
  override def update(project: Project) = withEvict(CACHE_REGION) { validUpdate(project, repository.save(_)) }

  @Transactional
  override def delete(project: Project) = withEvict(CACHE_REGION) {
    val currentTimeMillis = System.currentTimeMillis()
    val deletedProject = project.copy(
      name = project.name + "_" + currentTimeMillis.toString,
      isDeleted = true,
      removalTime = Some(currentTimeMillis)
    )
    update(deletedProject)
  }

  @Transactional(readOnly = true)
  def findByName(project: String): Option[Project] = {
    list.find(p => p.name == project)
  }

  @Transactional(readOnly = true)
  def getProjects(ids: Iterable[Int], ordering: Option[Ordering] = None): Iterable[Project] = getFromCache(ids.mkString("~") + ordering.map(o => s"*~${o.field}~${o.direction}").getOrElse("")){
    repository.getProjects(ids, ordering)
  }

  @Transactional(readOnly = true)
  def getProjectAdmins(projectId: Int): Seq[String] = {
    authorityService.getProjectAuthority(projectId, GenesisRole.ProjectAdmin) match {
      case Success((usernames, groups)) => usernames.toSeq
      case _ => Seq()//todo !!!
    }
  }
}
