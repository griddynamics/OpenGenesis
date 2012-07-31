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
package com.griddynamics.genesis.model

import WorkflowStepStatus._
import java.sql.Timestamp

//todo: consider adding retryCount & restarted fields to WorkflowStep class
class WorkflowStep(val workflowId : GenesisEntity.Id,
                   val phase      : String,
                   var status     : WorkflowStepStatus,
                   val details    : String,
                   val started    : Option[java.sql.Timestamp],
                   val finished   : Option[java.sql.Timestamp]) extends GenesisEntity{

    def this() = this (0, "", Requested, "", Some(new Timestamp(1)), Some(new Timestamp(1)))
}

object WorkflowStep{
    def apply(id         : GenesisEntity.Id,
              workflowId : GenesisEntity.Id,
              phase      : String,
              status     : WorkflowStepStatus,
              details    : String,
              started    : Option[java.sql.Timestamp] = None,
              finished   : Option[java.sql.Timestamp] = None) = {

        val workflowStep =
                new WorkflowStep(
                    workflowId,
                    phase,
                    status,
                    details,
                    started,
                    finished
                )

        workflowStep.id = id
        workflowStep
    }
}


