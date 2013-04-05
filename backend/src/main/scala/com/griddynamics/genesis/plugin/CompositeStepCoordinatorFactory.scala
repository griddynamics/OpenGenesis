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

package com.griddynamics.genesis.plugin

import com.griddynamics.genesis.workflow.Step
import com.griddynamics.genesis.util.Logging

class CompositeStepCoordinatorFactory(factories: Array[PartialStepCoordinatorFactory])
    extends StepCoordinatorFactory with Logging {

    def apply(step: Step, context: StepExecutionContext) = {
        factories.find(_.isDefinedAt(step)) match {
            case Some(factory) => try {
                factory.apply(step, context)
            } catch {
                case e: Throwable => {
                    log.error(e, "Failed to create step coordinator for %s".format(step))
                    throw new RuntimeException("Failed to start step %s due to error: %s".format(step, e.getCause match {
                        case null => e.getMessage
                        case _ => e.getCause.getMessage
                    }), e)
                }
            }
            case None => throw new RuntimeException("Failed to find coordinator for '%s'".
                format(step))
        }
    }
}
