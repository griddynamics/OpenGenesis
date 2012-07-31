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

/* Coordinator for flow execution process. One flow consists of
 * series of dependent steps. Each step is executed after all previous
 * was completed. Flow may be interrupted by arbitrary signal or finished
 * successfully. In a case of interruption all currently executed
 * StepCoordinator would be interrupted. All not started StepCoordinators
 * wouldn't be started. Interrupt signal may be send by FlowCoordinator
 * itself or from external entity.
 */

trait FlowCoordinator {
    def flowDescription: String

    /* Called once on step start to retrieve initial step coordinators */
    def onFlowStart(): Either[Signal, Seq[StepCoordinator]]

    /* Called on each when all steps or flow was interrupted */
    def onFlowFinish(signal: Signal)

    /* Called on each step completion to retrieve following step coordinators */
    def onStepFinish(result: StepResult): Either[Signal, Seq[StepCoordinator]]
}
