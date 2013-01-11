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
package com.griddynamics.genesis.configuration

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.security.acls.domain._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.acls.model._
import org.springframework.security.acls.jdbc.{JdbcMutableAclService, LookupStrategy, BasicLookupStrategy}
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.security.acls.AclPermissionEvaluator
import org.springframework.security.access.PermissionEvaluator
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.api.Identifiable
import scala.Some
import java.util
import com.griddynamics.genesis.users.GenesisRole
import com.griddynamics.genesis.cache.{CacheConfig, Cache, CacheManager}
import java.io.Serializable
import org.springframework.security.util.FieldUtils

@Configuration
class AclServiceContext {

  @Autowired var cacheManager: CacheManager = _
  @Autowired var dataSource: BasicDataSource  = _

  @Bean def aclAuthorizationStrategy: AclAuthorizationStrategy = {
    new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_GENESIS_ADMIN"))
  }

  @Bean def permissionGrantingStrategy: PermissionGrantingStrategy = {
    import scala.collection.JavaConversions._
    new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger) {
      override def isGranted(acl: Acl, permission: util.List[Permission], sids: util.List[Sid], administrativeMode: Boolean) = {
        if (sids.find(_ == new GrantedAuthoritySid(GenesisRole.SystemAdmin.toString)).isDefined)
          true
        else
          super.isGranted(acl, permission, sids, administrativeMode)
      }
    }

  }

  @Bean def aclCache: AclCache = new GenesisAclCache(
    "aclCache",
    cacheManager,
    permissionGrantingStrategy,
    aclAuthorizationStrategy
  )

  @Bean def lookupStrategy:LookupStrategy = new BasicLookupStrategy(dataSource, aclCache, aclAuthorizationStrategy, permissionGrantingStrategy)

  @Bean def aclService: MutableAclService = {
    val service = new JdbcMutableAclService(dataSource, lookupStrategy, aclCache)

    val jdbcUrl = dataSource.getUrl.toLowerCase
    if(jdbcUrl.startsWith("jdbc:mysql") || jdbcUrl.startsWith("jdbc:sqlserver")) {
      service.setClassIdentityQuery("SELECT @@IDENTITY")
      service.setSidIdentityQuery("SELECT @@IDENTITY")
    }

    service
  }

  @Bean def aclPermissionEvaluator: PermissionEvaluator = {
    val eval = new AclPermissionEvaluator(aclService)
    eval.setObjectIdentityRetrievalStrategy(objectRetrievalStategy)
    eval
  }

  @Bean def objectRetrievalStategy: ObjectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategy {
    def getObjectIdentity(domainObject: Any) = domainObject match {
      case env: Identifiable[_] => env.id match {
        case Some(id: Int) => new ObjectIdentityImpl(env.getClass, id)
        case id: Int => new ObjectIdentityImpl(env.getClass, id)
        case _ => throw new RuntimeException("Unsupported Identifiable type")
      }

      case obj => new ObjectIdentityImpl(obj)
    }
  }

  class GenesisAclCache(val cacheName: String,
                        val cacheManager: CacheManager,
                        val permissionGrantingStrategy: PermissionGrantingStrategy,
                        val aclAuthorizationStrategy: AclAuthorizationStrategy) extends AclCache {

    private def assureCacheExists() {
      cacheManager.createCacheIfAbsent(CacheConfig(cacheName, TimeUnit.HOURS.toSeconds(1).toInt, 100))
    }

    def evictFromCache(pk: Serializable) {
      assureCacheExists()

      val acl = getFromCache(pk)

      if (acl != null) {
        cacheManager.evictFromCache(cacheName, acl.getId)
        cacheManager.evictFromCache(cacheName, acl.getObjectIdentity)
      }
    }

    def evictFromCache(objectIdentity: ObjectIdentity) {
      evictFromCache(objectIdentity.asInstanceOf[Serializable])
    }

    def getFromCache(objectIdentity: ObjectIdentity) =
      getFromCache(objectIdentity.asInstanceOf[Serializable])

    def getFromCache(pk: Serializable) = {
      assureCacheExists()
      try {
        cacheManager.fromCache(cacheName, pk).map { value =>
          initializeTransientFields(value.asInstanceOf[MutableAcl])
        }.getOrElse(null.asInstanceOf[MutableAcl])
      } catch {
        case _ => null
      }
    }

    def putInCache(acl: MutableAcl) {
      assureCacheExists()
      if ((acl.getParentAcl != null) && (acl.getParentAcl.isInstanceOf[MutableAcl])) {
        putInCache(acl.getParentAcl.asInstanceOf[MutableAcl])
      }
      cacheManager.putInCache(cacheName, acl.getObjectIdentity, acl)
      cacheManager.putInCache(cacheName, acl.getId, acl)
    }

    def clearCache() {
      if (cacheManager.cacheExists(cacheName))
        cacheManager.clearCache(cacheName)
    }

    private def initializeTransientFields(value: MutableAcl): MutableAcl = {
      if (value.isInstanceOf[AclImpl]) {
        FieldUtils.setProtectedFieldValue("aclAuthorizationStrategy", value, this.aclAuthorizationStrategy)
        FieldUtils.setProtectedFieldValue("permissionGrantingStrategy", value, this.permissionGrantingStrategy)
      }
      if (value.getParentAcl != null) {
        initializeTransientFields(value.getParentAcl.asInstanceOf[MutableAcl])
      }
      value
    }
  }

}
