/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 * http://www.griddynamics.com
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project:     Genesis
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.rest.filters

import org.scalatest.FunSuite
import com.griddynamics.genesis.model.EnvStatus
import com.griddynamics.genesis.model.EnvStatus.EnvStatus

class EnvFilterTest extends FunSuite {

  test("Correct filter values") {
    val result = "statuses[ Ready  ,  Destroyed,Broken,  Busy,,, ]" match {
      case EnvFilter(statuses @ _*) => statuses
      case _ => Seq.empty[EnvStatus]
    }
    assert(result === Seq(EnvStatus.Ready, EnvStatus.Destroyed, EnvStatus.Broken, EnvStatus.Busy))
  }

  test("One filter value") {
    val result = "statuses[Destroyed, ]" match {
      case EnvFilter(statuses @ _*) => statuses
      case _ => Seq.empty[EnvStatus]
    }
    assert(result === Seq(EnvStatus.Destroyed))
  }

  test("Empty filter value") {
    assert(EnvFilter.unapplySeq("statuses[]") === Some(Seq.empty[EnvStatus]))
  }

  test("Non-existent status value") {
    assert(EnvFilter.unapplySeq("statuses[Ready,Non-existent,Failed,Busy]") === None)
  }

  test("Incorrect filter value") {
    assert(EnvFilter.unapplySeq("ready,failed]") === None)
  }
}
