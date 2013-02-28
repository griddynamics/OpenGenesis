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
import com.griddynamics.genesis.service.ProjectService
import org.aspectj.lang.annotation.{Pointcut, Before, Aspect}
import com.griddynamics.genesis.rest.ResourceNotFoundException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.CodeSignature
import com.griddynamics.genesis.api.GenesisService

@Aspect
class ProjectBoundaryCheckingAspect {
  @Autowired
  var projectService: ProjectService = _

  @Autowired var genesisService: GenesisService = _

  @Pointcut("within(@org.springframework.web.bind.annotation.RequestMapping *)")
  def requestMapping() {}

  private def getIntParam(name : String, params: Seq[(String, Any)]) = params.collectFirst {
    case (k, v: Int) if (k == name || k.startsWith(name + "$")) => v
  }

  private def checkProject(projectId: Int) {
    if (projectService.get(projectId).isEmpty) {
      throw new ResourceNotFoundException("Project [%d] wasn't found".format(projectId))
    }
  }

  private def checkEnv(projectId: Int, envId: Int) {
    if(!genesisService.isEnvExists(envId, projectId))
      throw new ResourceNotFoundException("Environment [id = %s] wasn't found in project [id = %s]".format(envId, projectId))
  }

  @Before(value = "within(@org.springframework.web.bind.annotation.RequestMapping *) && execution(* com.griddynamics.genesis.rest.*Controller.*(..))")
  def checkEnvProjectExists(joinPoint: JoinPoint) {
    val params = joinPoint.getSignature match {
      case sign: CodeSignature if sign.getParameterNames != null => sign.getParameterNames.zip(joinPoint.getArgs)
      case _ => return
    }
    getIntParam("projectId", params).foreach ( projId => {
      checkProject(projId)
      getIntParam("envId", params).foreach(checkEnv(projId, _))
    })
  }
}
