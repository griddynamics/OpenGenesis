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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.ldap

import com.griddynamics.genesis.cache.{CacheConfig, CacheManager}

private[ldap] trait WildcardCaching {
  def cacheManager: CacheManager

  def defaultTtl: Int
  def maxEntries: Int

  def fromCache[T](cache: String, wildcard: String, cacheFilter: T => Boolean)(callback: => List[T]): List[T] = {
    cacheManager.createCacheIfAbsent(CacheConfig(cache, defaultTtl, maxEntries))

    val finder = (key: Any) => key match {
      case key: String => Wildcard(key).accept(wildcard)
      case _ => false
    }

    cacheManager.fromCache(cache, finder) match {
      case Some(records) => records.asInstanceOf[List[T]].filter(cacheFilter)
      case None => {
        val records: List[T] = callback
        cacheManager.putInCache(cache, wildcard, records)
        records
      }
    }
  }
}
