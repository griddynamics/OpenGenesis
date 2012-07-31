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
package com.griddynamics.genesis.workflow.actor

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import java.util.Random
import com.griddynamics.genesis.workflow
import java.util.concurrent.{Executors, CountDownLatch}
import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.workflow.step.{ActionStep, ActionStepResult}
import workflow._
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicReference

class FlowElement(val action: Action, val precursors: Set[FlowElement]) {
    override def toString = "FlowElement(%s, %s)".format(action, precursors)
}

object FlowElement {
    def apply(action: Action, precursors: Set[FlowElement]) = new FlowElement(action, precursors)

    def apply(action: Action, precursors: FlowElement*) = new FlowElement(action, Set(precursors: _*))

    def unapply(flowElement: FlowElement): Option[(Action, Set[FlowElement])] = {
        Some((flowElement.action, flowElement.precursors))
    }
}

case class TestAction(name: String) extends Action

case class TestResult(action: TestAction) extends ActionResult

class TestCoordinator(flow: Set[FlowElement]) extends workflow.FlowCoordinator {
    val flowsToStart = mutable.Set[FlowElement](flow.view.toSeq: _*)
    val finishedFlows = mutable.Set[FlowElement]()

    var finishLatch = new CountDownLatch(1)

    var startFlow = new AtomicReference(Seq[String]())
    var finishFlow = Seq[String]()

    def flowDescription = "TestFlow"

    def onFlowStart() = Right(getReachableExecutors())

    def onStepFinish(result: StepResult) = {
        result match {
            case ActionStepResult(ActionStep(TestAction(actionName)), _) => {
                finishFlow = actionName +: finishFlow

                finishedFlows.add(flow.find {
                    case FlowElement(TestAction(an), _) if an == actionName => true
                    case _ => false
                }.get)

                Right(getReachableExecutors())
            }
        }
    }

    def getReachableExecutors() = {
        val reachableFlowElements = flowsToStart.filter(_.precursors subsetOf finishedFlows).toSeq

        flowsToStart --= reachableFlowElements

        reachableFlowElements.map {
            case FlowElement(action@TestAction(_), _) => new ActionStepCoordinator(
                new TestExecutor(action, this)
            )
        }
    }

    def onFlowFinish(signal: Signal) {
        startFlow.set(startFlow.get().reverse)
        finishFlow = finishFlow.reverse
        finishLatch.countDown
    }
}

class TestExecutor(val action: TestAction, testCoordinator: TestCoordinator) extends AsyncActionExecutor {
    var bestRemain = (new Random).nextInt(100)

    def getResult = {
        bestRemain -= 1

        if (bestRemain < 0)
            Some(TestResult(action))
        else
            None
    }

    def startAsync() {
        var oldStartFlow = testCoordinator.startFlow.get

        while (!testCoordinator.startFlow.compareAndSet(oldStartFlow, action.name +: oldStartFlow))
            oldStartFlow = testCoordinator.startFlow.get
    }

    def cleanUp(signal: Signal) {}
}

class SimpleFlowTest extends AssertionsForJUnit with Logging {

    import SimpleFlowTest._

    @Test
    def testEmptyWorkflow() {
        testWorkflow(Set())
    }

    @Test
    def testSingleWorkflow() {
        testWorkflow(Set(FlowElement(TestAction("A"))))
    }

    @Test
    def testSimpleWorkflow() {
        testWorkflow(Simple.sources)
    }

    @Test
    def testHardWorkflow() {
        testWorkflow(Hard.sources)
    }

    def testWorkflow(flow: Set[FlowElement]) {
        val testCoordinator = new TestCoordinator(flow)

        val flowCoordinator = new TypedFlowCoordinatorImpl(
            testCoordinator, 20, 10000, Executors.newSingleThreadExecutor
        )

        flowCoordinator.start()

        testCoordinator.finishLatch.await

        log.info("StartFlow - " + testCoordinator.startFlow)
        log.info("FinishFlow - " + testCoordinator.finishFlow)

        for ((flowName, stringFlow) <- List(("StartFlow", testCoordinator.startFlow.get), ("FinishFlow", testCoordinator.finishFlow)))
            for (flowElement <- flow)
                assert(
                    isFlowSatisfied(stringFlow, flowElement),
                    "Flow %s isn't satisfied in %s".format(flowToString(flowElement.precursors), flowName)
                )
    }
}

object SimpleFlowTest {
    def isFlowSatisfied(stringFlow: Seq[String], flowElement: FlowElement): Boolean = {
        val actionName = flowElement.action.asInstanceOf[TestAction].name

        val beforeActions = flowElement.precursors.map(_.action.asInstanceOf[TestAction].name).toSet

        val observedActions = stringFlow.takeWhile(_ != actionName).toSet

        (beforeActions subsetOf observedActions) && stringFlow.contains(actionName)
    }

    def flowToString(precursors: Set[FlowElement]) =
        precursors.map(_.action.asInstanceOf[TestAction].name).mkString("[", ",", "]")

    // A
    //   -> C -> D
    // B
    object Simple {
        val A = FlowElement(TestAction("A"))
        val B = FlowElement(TestAction("B"))
        val C = FlowElement(TestAction("C"), A, B)
        val D = FlowElement(TestAction("D"), C)

        val sources = Set(A, B, C, D)
    }

    // A -> B         G -> H
    //      C -> F ->
    // D -> E         I -> J
    object Hard {
        val A = FlowElement(TestAction("A"))
        val D = FlowElement(TestAction("D"))
        val C = FlowElement(TestAction("C"))

        val B = FlowElement(TestAction("B"), A)
        val E = FlowElement(TestAction("E"), D)

        val F = FlowElement(TestAction("F"), B, C, E)

        val G = FlowElement(TestAction("G"), F)
        val I = FlowElement(TestAction("I"), F)

        val H = FlowElement(TestAction("H"), G)
        val J = FlowElement(TestAction("J"), I)

        val sources = Set(A, B, C, D, E, F, G, H, I, J)
    }

}