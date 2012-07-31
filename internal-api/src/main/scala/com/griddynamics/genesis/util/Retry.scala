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

import java.util.concurrent.TimeoutException
import annotation.tailrec

trait Retriable {
   def retryCount : Int
   def retryDelay: Int
   def retry[B](block : => B) :B  = Retry.retryWithCount(retryCount, retryDelay)(block)
}

object Retry extends Logging {
    def retryWithTimeout[T](timeoutMillis : Long, sleepMillis : Long)
                        (block : => Option[T]) : T = {
        retryWithTimeout(timeoutMillis, sleepMillis, java.lang.System.nanoTime(), block)
    }

    @tailrec
    private def retryWithTimeout[T](timeoutMillis : Long, sleepMillis : Long, startTime : Long, block : => Option[T]) : T = {
        val currentTime = java.lang.System.nanoTime()
        if ((currentTime - startTime) > timeoutMillis * 1000000)
            throw new TimeoutException()

        val res = block
        if (res.isDefined)
            res.get
        else {
            Thread.sleep(sleepMillis)
            retryWithTimeout(timeoutMillis, sleepMillis, startTime, block)
        }
    }

    def retryWithCount[T](retryCount: Int, delay: Long = 100)(block : => T) : T = {
        try {block} catch {
            case e if retryCount > 1 => {
                log.error(e, "Error running block. Will retry it. Retry count: %d",  retryCount)
                Thread.sleep(delay)
                retryWithCount(retryCount - 1, delay)(block)
            }
        }
    }
}