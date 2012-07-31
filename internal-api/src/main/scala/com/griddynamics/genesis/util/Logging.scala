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
package com.griddynamics.genesis.util

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait Logging {
    protected implicit val log = new RichLogger(LoggerFactory.getLogger(this.getClass))
}

class RichLogger(val logger : Logger) {
    def info(message : String, args : Any*) {
        if (logger.isInfoEnabled)
            if (args.isEmpty)
                logger.info(message)
            else
                logger.info(message.format(args : _*))
    }

    def info(throwable : Throwable, message : String, args : Any*) {
        if (logger.isInfoEnabled)
            if (args.isEmpty)
                logger.info(message, throwable)
            else
                logger.info(message.format(args : _*), throwable)
    }

    def warn(message : String, args : Any*) {
        if (logger.isWarnEnabled)
            if (args.isEmpty)
                logger.warn(message)
            else
                logger.warn(message.format(args : _*))
    }

    def warn(throwable : Throwable, message : String, args : Any*) {
        if (logger.isWarnEnabled)
            if (args.isEmpty)
                logger.warn(message, throwable)
            else
                logger.warn(message.format(args : _*), throwable)
    }

    def error(message : String, args : Any*) {
        if (logger.isErrorEnabled)
            if (args.isEmpty)
                logger.error(message)
            else
                logger.error(message.format(args : _*))
    }

    def error(throwable : Throwable, message : String, args : Any*) {
        if (logger.isErrorEnabled)
            if (args.isEmpty)
                logger.error(message, throwable)
            else
                logger.error(message.format(args : _*), throwable)
    }

    def debug(message : String, args : Any*) {
        if (logger.isDebugEnabled)
            if (args.isEmpty)
                logger.debug(message)
            else
                logger.debug(message.format(args : _*))
    }

    def debug(throwable : Throwable, message : String, args : Any*) {
        if (logger.isDebugEnabled)
            if (args.isEmpty)
                logger.debug(message, throwable)
            else
                logger.debug(message.format(args : _*), throwable)
    }

    def trace(message : String, args : Any*) {
        if (logger.isTraceEnabled)
            if (args.isEmpty)
                logger.trace(message)
            else
                logger.trace(message.format(args : _*))
    }

    def trace(throwable : Throwable, message : String, args : Any*) {
        if (logger.isTraceEnabled)
            if (args.isEmpty)
                logger.trace(message, throwable)
            else
                logger.trace(message.format(args : _*), throwable)
    }
}

object RichLogger {
    implicit def richLoggerToLogger(logger : RichLogger) : Logger = logger.logger
}
