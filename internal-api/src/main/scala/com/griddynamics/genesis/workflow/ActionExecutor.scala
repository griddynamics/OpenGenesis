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
package com.griddynamics.genesis.workflow

import com.griddynamics.genesis.util.Logging
import java.util.concurrent.{TimeUnit, Future, Callable, ExecutorService}
import java.util.concurrent.atomic.AtomicReference
import com.griddynamics.genesis.workflow.signal.Success
import com.griddynamics.genesis.logging.LoggerWrapper

/* Base trait for classes for particular actions executors */
trait ActionExecutor {
    val action: Action

    /** Any way called by framework when action execution
     *  was finished or interrupted by some signal
     *
     *  @param signal interrupt signal or
     *  Success() in a case of normal finish
     */
    def cleanUp(signal: Signal)
    def canRespond(signal : Signal) : Boolean = true
}

/* Executor for performing asynchronous actions */
trait AsyncActionExecutor extends ActionExecutor {
    /* Called once to start particular async action */
    def startAsync()

    /* Called periodically until Some(result) returned */
    def getResult(): Option[ActionResult]
}

/* Executor for performing synchronous actions */
trait SyncActionExecutor extends ActionExecutor {
    /* Called once to get result of synchronous action.
     * Executing thread may be interrupted in a case of
     * action interrupt, so implementations must check
     * thread interrupt status and be ready to process
     * InterruptedException
     */
    def startSync(): ActionResult
}

/* Adapter class from SyncActionExecutor to AsyncActionExecutor
 * using ExecutorService to perform action and pull its status
 */
class SyncActionExecutorAdapter(syncExecutor: SyncActionExecutor,
                                executorService: ExecutorService) extends AsyncActionExecutor
with Logging {
    val action = syncExecutor.action
    val signal = new AtomicReference[Signal](Success())

    val task = new Callable[ActionResult] {
        def call() = {
            try {
                syncExecutor.startSync()
            }
            finally {
                try {
                    syncExecutor.cleanUp(signal.get())
                }
                catch {
                    case t => log.warn(t, "Throwable while invoking cleanUp for '%s'", action)
                }
            }
        }
    }

    var future: Future[ActionResult] = _

    def startAsync() {
        future = executorService.submit(task)
    }

    def getResult() = {
        future.isDone match {
            case true => Some(future.get) // possible exception throw
            case false => None
        }
    }

    def cleanUp(signal: Signal) {
        this.signal.set(signal)
        future.cancel(true)
    }
}

/* Empty method stubs for ActionExecutor */
trait SimpleActionExecutor extends ActionExecutor {
    def cleanUp(signal: Signal) {}
}

/* Empty method stubs for AsyncActionExecutor */
trait SimpleAsyncActionExecutor extends SimpleActionExecutor with AsyncActionExecutor

/* Empty method stubs for SyncActionExecutor */
trait SimpleSyncActionExecutor extends SimpleActionExecutor with SyncActionExecutor

/* Async executor that may be timed out */
trait AsyncTimeoutAwareActionExecutor extends AsyncActionExecutor {
    /* Timeout value in milliseconds */
    def timeoutMillis : Long

    /* Called after timeout when getResult returned None */
    def getResultOnTimeout : ActionResult

}

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
                LoggerWrapper.writeLog(action.uuid, timeoutDescription)
                log.debug("action '%s' is timed out, limit: %dmsec", action, timeoutMillis)
                Some(getResultOnTimeout)
            }
        }
    }
}
