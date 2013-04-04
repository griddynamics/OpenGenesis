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

package com.griddynamics.genesis.workflow

import com.griddynamics.genesis.util.Logging
import java.util.concurrent.TimeUnit
import com.griddynamics.genesis.logging.LoggerWrapper

/* Trait to mixin in AsyncTimeoutAwareActionExecutor
 * implementations to support timeout behaviour
 */
trait DurationLimitedActionExecutor extends AsyncTimeoutAwareActionExecutor with Logging {
    var startTimeNanos : Long = _

    private def isTimedOut = {
        val durationNanos = java.lang.System.nanoTime() - startTimeNanos
        val timeoutNanos = timeoutMillis * 1000000
        durationNanos > timeoutNanos
    }

    abstract override def startAsync() {
        startTimeNanos = java.lang.System.nanoTime()
        super.startAsync()
    }

    def timeoutDescription = "WARNING: Action '%s' timed out. Time out was set to [%d seconds]".format(action.desc, TimeUnit.MILLISECONDS.toSeconds(timeoutMillis).toInt)

    abstract override def getResult() = {
        (super.getResult(), isTimedOut) match {
            case (Some(ar), _) => Some(ar)
            case (None, false) => None
            case (None, true) => {
                LoggerWrapper.writeActionLog(action.uuid, timeoutDescription)
                log.debug("action '%s' is timed out, limit: %dmsec", action, timeoutMillis)
                Some(getResultOnTimeout)
            }
        }
    }
}
