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
package com.griddynamics.genesis.core

import scala.collection.mutable
import com.griddynamics.genesis.model.GenesisEntity
import java.lang.RuntimeException
import com.griddynamics.genesis.plugin.{StepBuilder, GenesisStep}


class CyclicFlowException(val flowSteps: Seq[StepBuilder]) extends RuntimeException

class FlowStepsTemplate(val flowSteps: Seq[StepBuilder]) {
    val successionMap = {
        import scala.collection.JavaConversions._
        val result = mutable.Map[GenesisEntity.Id, Seq[StepBuilder]]()

        var stepsToStart = flowSteps

        while (result.size != flowSteps.size) {
            val (readySteps, unreadySteps) = stepsToStart.partition {
                step => step.precedingPhases.forall {
                    phase => stepsToStart.find(_.phase == phase).isEmpty
                }
            }

            if (readySteps.size == 0)
                throw new CyclicFlowException(flowSteps)

            stepsToStart = unreadySteps

            for (step <- readySteps) {
                val directBehindSteps = flowSteps.filter(s => step.precedingPhases.contains(s.phase))
                val behindStepsIds = directBehindSteps.flatMap(s => result(s.id).map(s => s.id) :+ s.id).toSet
                val behindSteps = flowSteps.filter(s => behindStepsIds.contains(s.id))

                result(step.id) = behindSteps
            }
        }

        result.toMap
    }

    def isStepsParallel(firstId: GenesisEntity.Id, secondId: GenesisEntity.Id) = {
        successionMap(firstId).find(_.id == secondId).isEmpty &&
            successionMap(secondId).find(_.id == firstId).isEmpty
    }

    def isStepsConsecutive(firstId: GenesisEntity.Id, secondId: GenesisEntity.Id) =
        !isStepsParallel(firstId, secondId)
}
