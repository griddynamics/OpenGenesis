package com.griddynamics.genesis.rest

import org.springframework.web.bind.annotation.ExceptionHandler._
import org.springframework.web.bind.annotation.{ResponseStatus, ExceptionHandler}
import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletResponse

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
 * @Project:     Genesis
 * @Description: Execution Workflow Engine
 */
trait RestApiExceptionsHandler {

  @ExceptionHandler(value = Array(classOf[InvalidInputException]))
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  def handleInvalidParams(response : HttpServletResponse) {
    response.getWriter.write("{\"error\": \"Invalid input\"}")
  }

  @ExceptionHandler(value = Array(classOf[MissingParameterException]))
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  def handleMissingParam(response : HttpServletResponse, exception: MissingParameterException) {
    response.getWriter.write("{\"error\": \"Missing parameter: %s\"}".format(exception.paramName))
  }

  @ExceptionHandler(value = Array(classOf[ResourceNotFoundException]))
  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "resource not found")
  def handleResourceNotFound(response : HttpServletResponse, exception: ResourceNotFoundException) {
    /*do nothing*/
  }

}
