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

package com.griddynamics.genesis.service.impl

import com.griddynamics.genesis.service.{TemplateRepoService, ConfigService, StoreService => SS}
import com.griddynamics.genesis.cache.Cache
import com.griddynamics.genesis.util.Logging
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.template.{Modes, TemplateRepositoryFactory}
import com.griddynamics.genesis.api.TemplateRepo

class TemplateRepoServiceImpl(config: ConfigService, storeService: SS,
                              factories: Seq[TemplateRepositoryFactory],
  val cacheManager: CacheManager) extends TemplateRepoService  with Logging with Cache {

  private val PREFIX = "genesis.template.repository"
  private val PROPERTY_MODE = PREFIX + ".mode"
  private val CACHE_REGION = "ProjectTemplateRepositories"

  private def getMode(projectId: Int) = config.get(projectId, PROPERTY_MODE, "classpath")

  // never expire:
  override val eternal = true
  override val defaultTtl = 0

  def get(projectId: Int) = {
    fromCache(CACHE_REGION, projectId) {
      val mode = getMode(projectId)
      log.debug("Project id=%d uses template repository mode = %s", projectId, mode)
      factories.find(Modes.withName(mode) == _.mode).map(_.newTemplateRepository(projectId))
        .getOrElse(throw new IllegalArgumentException("%s template repository not found for project [id=%d]"
        .format(mode, projectId)))
    }
  }

  def listModes = factories.map(_.mode)

  def listSettings(mode: Modes.Mode) = TemplateRepo(mode.toString, factories.find(mode == _.mode)
    .map(_.settings).getOrElse(Seq())) // TODO: is factory a right place for a list of possible settings?

  def getConfig(projectId: Int) = {
    val tr = listSettings(Modes.withName(getMode(projectId)))
    val ro = storeService.listEnvs(projectId).nonEmpty
    TemplateRepo(tr.mode, tr.configuration.map(cp => cp.copy(value = config.get(projectId, cp.name, cp.value), readOnly = ro)))
  }

  def updateConfig(projectId: Int, settings: Map[String, Any]) {
    // config property names should NOT already contain project prefix
    settings.foreach { case (name, value) => config.update(projectId, name, value) }
    if (cacheManager.cacheExists(CACHE_REGION)) {
      cacheManager.getCache(CACHE_REGION).remove(projectId)
    }
  }
}
