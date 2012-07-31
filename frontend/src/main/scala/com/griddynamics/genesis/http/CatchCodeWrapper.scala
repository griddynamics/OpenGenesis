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

package com.griddynamics.genesis.http

import javax.servlet.http.{HttpServletResponseWrapper, HttpServletResponse}
import com.griddynamics.genesis.util.Logging


class CatchCodeWrapper(response: HttpServletResponse, val codes:Array[Int] = Array(404)) extends HttpServletResponseWrapper(response) with Logging {
    def willServe = {
        codes.contains(statusCode)
    }

    var statusCode: Int = _
    var message: Option[String] = None
    var shouldSendError: Boolean = false

    override def setStatus(sc: Int) {
        checkStatus(sc, None)
        super.setStatus(sc)
    }

    override def setStatus(sc: Int, sm: String) {
        checkStatus(sc, Some(sm))
        super.setStatus(sc)
    }


    override def sendError(sc: Int, msg: String) {
        shouldSendError = true
        if (checkStatus(sc, Some(msg)))
            super.sendError(sc, msg)
    }

    override def sendError(sc: Int) {
        shouldSendError = true
        if (checkStatus(sc, None)) {
              super.sendError(sc)
        }
    }

    def resume() {
        if (!response.isCommitted && shouldSendError) {
            message match {
                case None => getResponse.asInstanceOf[HttpServletResponse].sendError(statusCode)
                case Some(s) => getResponse.asInstanceOf[HttpServletResponse].sendError(statusCode, s)
            }
        }
    }

    private def checkStatus(sc: Int, msg: Option[String]): Boolean = {
        statusCode = sc
        message = msg
        ! codes.contains(sc)
    }
}
