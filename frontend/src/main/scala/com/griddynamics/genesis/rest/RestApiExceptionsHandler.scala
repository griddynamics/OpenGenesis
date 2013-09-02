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
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.rest

import com.griddynamics.genesis.api.Failure
import com.griddynamics.genesis.util.Logging
import javax.servlet.http.HttpServletResponse
import net.liftweb.json.{Serialization, MappingException}
import org.springframework.http.{MediaType, HttpStatus}
import org.springframework.validation.{ObjectError, FieldError}
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.{ResponseStatus, ExceptionHandler}
import org.springframework.web.context.request.WebRequest
import org.springframework.security.access.AccessDeniedException

trait RestApiExceptionsHandler extends Logging {
    implicit val formats = net.liftweb.json.DefaultFormats

    @ExceptionHandler(value = Array(classOf[InvalidInputException]))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    def handleInvalidParams(response : HttpServletResponse,e :InvalidInputException) {
      e.msg match {
        case Some(msg) => response.getWriter.write("{\"error\": \"%s\"}".format(msg))
        case None => response.getWriter.write("{\"error\": \"Invalid input\"}")
      }

    }

    @ExceptionHandler(value = Array(classOf[MissingParameterException]))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    def handleMissingParam(response : HttpServletResponse, exception: MissingParameterException) {
        response.getWriter.write("{\"error\": \"Missing parameter: %s\"}".format(exception.paramName))
    }

    @ExceptionHandler(value = Array(classOf[ResourceConflictException]))
    @ResponseStatus(HttpStatus.CONFLICT)
    def handleConflict(response : HttpServletResponse, exception: ResourceConflictException) {
      response.getWriter.write("{\"error\":\"" + exception.msg + "\"}")
    }

    @ExceptionHandler(value = Array(classOf[MappingException]))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    def handleMappingException(response: HttpServletResponse, exception: MappingException) {
      response.getWriter.write("{\"error\":\"" + exception.msg + "\"}")
    }

    @ExceptionHandler(value = Array(classOf[ResourceNotFoundException]))
    @ResponseStatus(HttpStatus.NOT_FOUND)
    def handleResourceNotFound(response : HttpServletResponse, exception: ResourceNotFoundException) {
      val writer = response.getWriter
      response.setContentType(MediaType.APPLICATION_JSON.toString)
      writer.write(Serialization.write(new Failure(isNotFound = true, compoundServiceErrors = List(exception.msg))))
    }

    @ExceptionHandler(value = Array(classOf[UnsupportedOperationException]))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    def handleUnsupported(response : HttpServletResponse, exception: UnsupportedOperationException) {
        response.getWriter.write("{\"error\":\"Operation not supported\"}")
    }

    @ExceptionHandler(value = Array(classOf[MethodArgumentNotValidException]))
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    def handleValidation(response : HttpServletResponse, exception: MethodArgumentNotValidException) {
      import scala.collection.JavaConversions._
      val errors = exception.getBindingResult.getAllErrors
      val fieldErrors = errors.collect { case error: FieldError =>
        ( error.getField, error.getDefaultMessage)
      }
      val servErrors = errors.collect {case error: ObjectError if !error.isInstanceOf[FieldError] => error.getDefaultMessage }

      response.getWriter.write(Serialization.write(
        new Failure(variablesErrors = fieldErrors.toMap, compoundServiceErrors = servErrors))
      )
    }

    @ExceptionHandler(value = Array(classOf[AccessDeniedException]))
    @ResponseStatus(HttpStatus.FORBIDDEN)
    def handleForbidden()(response: HttpServletResponse, exception: AccessDeniedException) {
       val writer = response.getWriter
       response.setContentType(MediaType.APPLICATION_JSON_VALUE)
       writer.write(Serialization.write(Failure(compoundServiceErrors = Seq(exception.getMessage))))
    }

    @ExceptionHandler(value = Array(classOf[Exception]))
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    def handleOtherExceptions(request: WebRequest, response : HttpServletResponse, exception: Exception) {
      log.error(exception, exception.getMessage)

      val acceptMediaTypes = MediaType.parseMediaTypes(request.getHeader("Accept"))

      if(acceptMediaTypes.contains(MediaType.APPLICATION_JSON)){
        val writer = response.getWriter //force WRITER mode on response
        response.setContentType(MediaType.APPLICATION_JSON.toString)
        writer.write("{\"error\": \"%s\"}".format(exception.getMessage))
      } else {
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error occurred")
      }
    }

}
