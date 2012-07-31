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
package com.griddynamics.genesis.spring.aop

import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.service.impl.ProjectService
import org.aspectj.lang.annotation.{Pointcut, Before, Aspect}
import com.griddynamics.genesis.rest.ResourceNotFoundException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.CodeSignature

@Aspect
class ProjectBoundaryCheckingAspect {
  @Autowired
  var projectService: ProjectService = _

  @Pointcut("within(@org.springframework.web.bind.annotation.RequestMapping *)")
  def requestMapping() {}

  @Before(value = "within(@org.springframework.web.bind.annotation.RequestMapping *) && execution(* com.griddynamics.genesis.rest.*Controller.*(..))")
  def checkProjectExists(joinPoint: JoinPoint) {
    val paramNames = joinPoint.getSignature match {
      case sign: CodeSignature => sign.getParameterNames
      case _ => null
    }

    if(paramNames != null) {
      val index = paramNames.indexWhere { it => it.equals("projectId") || it.startsWith("projectId$") } //scala adjusts argument names  with _${int}
      if(index != -1) {
        val projectId = joinPoint.getArgs.apply(index).asInstanceOf[Int]
        if (projectService.get(projectId).isEmpty) {
          throw new ResourceNotFoundException("Project [%d] wasn't found".format(projectId))
        }
      }
    }
  }
}
