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
package com.griddynamics.genesis.cache

import net.sf.ehcache.config.CacheConfiguration
import net.sf.ehcache.{CacheManager => EhManager, Element, Ehcache}

case class CacheConfig(name: String, ttl: Int, maxEntries: Int)

trait CacheManager {
  def createCacheIfAbsent(config: CacheConfig) {
    if (!cacheExists(config.name))
      synchronized {
        if (!cacheExists(config.name))
          createCache(config)
      }
  }

  protected def createCache(config: CacheConfig)

  def cacheExists(name: String): Boolean

  def fromCache(name: String, key: Any): Option[Any]

  def fromCache(name: String, keyPredicate: (Any) => Boolean): Option[Any]

  def putInCache(cacheName: String, key: Any, value: Any)

  def evictFromCache(cacheName: String, key: Any)

  def clearCache(cacheName: String)

  def removeFromCache(cacheName: String, filter: (Any) => Boolean)
}

object NullCacheManager extends CacheManager {
  protected def createCache(config: CacheConfig) {}

  def cacheExists(name: String) = true

  def fromCache(name: String, key: Any) = None

  def fromCache(name: String, keyPredicate: (Any) => Boolean) = None

  def putInCache(cacheName: String, key: Any, value: Any) {}

  def evictFromCache(cacheName: String, key: Any) {}

  def clearCache(cacheName: String) {}

  def removeFromCache(cacheName: String, filter: (Any) => Boolean) {}
}

class EhCacheManager(val manager: EhManager) extends CacheManager {

  protected def createCache(config: CacheConfig) {
    if (cacheExists(config.name))
      throw new IllegalArgumentException("Cache with name '%s' already exists".format(config.name))

    val configuration = new CacheConfiguration()
    configuration.setDiskPersistent(false)
    configuration.setEternal(config.ttl < 0)
    configuration.setMaxElementsOnDisk(0)
    configuration.setMaxEntriesLocalDisk(0)
    configuration.setMaxEntriesLocalHeap(config.maxEntries)
    configuration.setName(config.name)
    configuration.setTimeToIdleSeconds(config.ttl)
    configuration.setTimeToLiveSeconds(config.ttl)
    val cache: Ehcache = new net.sf.ehcache.Cache(configuration)
    manager.addCache(cache)
  }

  def cacheExists(name: String) = manager.cacheExists(name)

  def fromCache(name: String, key: Any) = {
    assertCacheExists(name)
    val cache = manager.getCache(name)

    Option(cache.get(key)) map {_.getObjectValue}
  }

  def fromCache(name: String, keyPredicate: (Any) => Boolean) = {
    import scala.collection.JavaConversions.asScalaBuffer

    assertCacheExists(name)
    val cache = manager.getCache(name)

    val keyOpt = cache.getKeysWithExpiryCheck.find(keyPredicate(_))

    keyOpt flatMap { key =>
      Option(cache.get(key)) map {_.getObjectValue}
    }
  }

  def putInCache(cacheName: String, key: Any, value: Any) {
    assertCacheExists(cacheName)
    val cache = manager.getCache(cacheName)

    cache.put(new Element(key, value))
  }

  def evictFromCache(cacheName: String, key: Any) {
    assertCacheExists(cacheName)
    manager.getCache(cacheName).remove(key)
  }

  def clearCache(cacheName: String) {
    assertCacheExists(cacheName)
    manager.getCache(cacheName).removeAll()
  }

  def removeFromCache(cacheName: String, filter: (Any) => Boolean) {
    import scala.collection.JavaConversions.{asScalaBuffer, asJavaCollection}

    assertCacheExists(cacheName)
    val cache = manager.getCache(cacheName)
    cache.removeAll(cache.getKeys.filter(filter))
  }

  private def assertCacheExists(name: String) {
    if (!cacheExists(name))
      throw new IllegalArgumentException("Cache with name '%s' doesn't exist".format(name))
  }

}
