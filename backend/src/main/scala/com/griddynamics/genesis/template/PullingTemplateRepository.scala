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
package com.griddynamics.genesis.template

import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.TimeUnit
import java.lang.System.currentTimeMillis
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.cache.Cache
import net.sf.ehcache.CacheManager

class PullingTemplateRepository(val delegate : TemplateRepository,
                                pullPeriodSeconds : Long,
                                pullOnStart : Boolean,
                                override val cacheManager : CacheManager) extends TemplateRepository
                                                          with InitializingBean with Logging with Cache {
    val pullPeriodMillis = TimeUnit.SECONDS.toMillis(pullPeriodSeconds)

    var sources = Map[VersionedTemplate, String]()
    var lastPullTimeMillis : Long = 0

    def afterPropertiesSet() {
        if (pullOnStart)
            pullSources()
    }

    def pullSources() {
        sources = delegate.listSources()
        lastPullTimeMillis = currentTimeMillis()
    }

    def listSources() = {
        val lastPullTimeInterval = currentTimeMillis() - lastPullTimeMillis

        if (lastPullTimeInterval > pullPeriodMillis)
            pullSources()

        sources
    }

    override def getContent(vt: VersionedTemplate) = sources.get(vt).orElse(delegate.getContent(vt)) //TODO: cache?
}
