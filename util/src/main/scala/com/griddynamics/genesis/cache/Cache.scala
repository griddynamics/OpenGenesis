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
package com.griddynamics.genesis.cache

trait Cache {
  def cacheManager: CacheManager

  def defaultTtl: Int = 30 //seconds
  def maxEntries: Int = 1000

  def fromCache[B](region: String, key: Any)(callback: => B): B = {
    cacheManager.createCacheIfAbsent(CacheConfig(region, defaultTtl, maxEntries))

    cacheManager.fromCache(region, key) map { _.asInstanceOf[B] } getOrElse {
      val toCache = callback
      (toCache == null || (toCache.isInstanceOf[Option[Any]] && !toCache.asInstanceOf[Option[Any]].isDefined)) match {
        case true =>
        case false => {
          cacheManager.putInCache(region, key, toCache)
        }
      }
      toCache
    }
  }

  def withEvict[B](region: String, key: AnyRef)(block: => B): B = {
    if (cacheManager.cacheExists(region))
      cacheManager.evictFromCache(region, key)
    block
  }
}