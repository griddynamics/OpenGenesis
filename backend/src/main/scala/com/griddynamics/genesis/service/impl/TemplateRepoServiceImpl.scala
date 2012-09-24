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

import com.griddynamics.genesis.service.{TemplateRepoService, ConfigService}
import com.griddynamics.genesis.cache.Cache
import com.griddynamics.genesis.util.Logging
import net.sf.ehcache.CacheManager
import com.griddynamics.genesis.template.{Modes, TemplateRepositoryFactory}

class TemplateRepoServiceImpl(config: ConfigService, templateRepoFactories: Seq[TemplateRepositoryFactory],
  val cacheManager: CacheManager) extends TemplateRepoService  with Logging with Cache {

  private val PROPERTY_MODE = "genesis.template.repository.mode"

  def get(projectId: Int) = fromCache("ProjectTemplateRepositories", Integer.valueOf(projectId)){
    val mode = config.get(projectId, PROPERTY_MODE, "classpath")
    log.debug("Project id=%d uses template repository mode = %s", projectId, mode)
     templateRepoFactories.find(Modes.withName(mode)  == _.mode).map(_.newTemplateRepository(projectId))
      .getOrElse(throw new IllegalArgumentException("%s template repository not found for project [id=%d]"
      .format(mode, projectId)))
  }

  // never expire:
  override val eternal = true
}
