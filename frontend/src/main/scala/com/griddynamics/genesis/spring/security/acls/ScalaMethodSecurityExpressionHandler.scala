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
 * Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.spring.security.acls

import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.expression.{EvaluationContext, Expression}
import org.springframework.security.access.expression.{SecurityExpressionRoot, ExpressionUtils}
import org.springframework.security.access.PermissionCacheOptimizer

class ScalaMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

  var permissionCacheOptimizer: Option[PermissionCacheOptimizer] = None

  override def filter(filterTarget: Any, filterExpression: Expression, ctx: EvaluationContext) = {
    filterTarget match {
      case seq: Iterable[_] => filterScalaCollection(seq, filterExpression, ctx)
      case _ => super.filter(filterTarget, filterExpression, ctx)
    }
  }

  def filterScalaCollection(filterTarget: Iterable[_], filterExpression: Expression, ctx: EvaluationContext) = {
    import scala.collection.JavaConversions._

    val rootObject = ctx.getRootObject.getValue.asInstanceOf[SecurityExpressionRoot]
    permissionCacheOptimizer.foreach { _.cachePermissionsFor(rootObject.getAuthentication, filterTarget) }

    val accessor = rootObject.asInstanceOf[{ def setFilterObject(o: AnyRef) }]
    filterTarget.filter { filterObject =>
      accessor.setFilterObject(filterObject.asInstanceOf[AnyRef])
      ExpressionUtils.evaluateAsBoolean(filterExpression, ctx)
    }
  }

  override def setPermissionCacheOptimizer(permissionCacheOptimizer: PermissionCacheOptimizer) {
    super.setPermissionCacheOptimizer(permissionCacheOptimizer)
    this.permissionCacheOptimizer = Option(permissionCacheOptimizer)
  }
}


