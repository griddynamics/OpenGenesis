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
 * Description: Continuous Delivery Platform
 */
package com.griddynamics.genesis.util

import org.junit.{Ignore, Test}
import scala.reflect.runtime.universe.{typeOf, TypeRef}
import org.scalatest.junit.AssertionsForJUnit

class ScalaReflectionUtilsTest extends AssertionsForJUnit {

  @Test def shouldFindPrimaryConstructor() {
    val (const, params) = ScalaReflectionUtils.getPrimaryConstructor(typeOf[SomeOrdinaryCaseClass])
    expectResult(3)(params.size)
    val paramsMap = params.toMap

    expectResult(typeOf[Int])(paramsMap("intVar"))
    expectResult(typeOf[String])(paramsMap("stringVar"))

    val listType = paramsMap("list")
    assert(listType.isInstanceOf[TypeRef])
    expectResult(typeOf[Seq[Int]])(listType)
    expectResult(typeOf[Int])(listType.asInstanceOf[TypeRef].args(0))
  }

  @Test def newInstanceCreation() {
    val constParams: Map[String, Any] = Map("intVar" -> 1, "stringVar" -> "stringValue", "list" -> Seq(3, 2, 1))
    val instance = ScalaReflectionUtils.newInstance(typeOf[SomeOrdinaryCaseClass], constParams)

    expectResult(SomeOrdinaryCaseClass(intVar = 1, stringVar = "stringValue", list = Seq(3, 2, 1)))(instance)
  }

  @Test(expected = classOf[Exception])
  @Ignore("type parameters check are not imlemented yet. Also in scala 2.10 MethodMirror they're not checked and instance is created with wrong type.")
  def newInstanceShouldRespectTypeParameters() {
    val instance = ScalaReflectionUtils.newInstance(typeOf[OptionsTestCase], Map("opt" -> Some("not a number")))
  }

  @Test def newInstanceShouldUseDefaultValues() {
    val instance = ScalaReflectionUtils.newInstance(typeOf[CaseClassWithDefaults], Map("stringVar" -> "some"))
    expectResult(CaseClassWithDefaults(stringVar = "some"))(instance)
  }

  @Test def newInstanceShouldOverrideDefaultValuesIfProvided() {
    val instance = ScalaReflectionUtils.newInstance(typeOf[CaseClassWithDefaults], Map("stringVar" -> "some", "intVar" -> 666))
    expectResult(CaseClassWithDefaults(intVar = 666, stringVar = "some"))(instance)
  }

  @Test def collectDefaultValuesShouldReturnConsDefaultValues() {
    val defaults = ScalaReflectionUtils.constructorDefaultValues(typeOf[CaseClassWithDefaults])

    expectResult(1)(defaults("intVar"))
    expectResult(Seq(1, 2, 3))(defaults("list").asInstanceOf[Seq[Int]])
    assert(defaults.get("stringVar").isEmpty)
  }

  @Test def optionOfSeqOfInts() {
    val (construct, params) = ScalaReflectionUtils.getPrimaryConstructor(typeOf[OptionOfSeqCaseClass])
    val map = params.toMap

    val optStringType = map("optString")
    assert(optStringType.isInstanceOf[TypeRef])
    val typeRef = optStringType.asInstanceOf[TypeRef]
    expectResult(typeOf[Option[String]])(optStringType)
    expectResult(typeOf[String])(typeRef.args(0))

    val optSeqType = map("optseq")

    expectResult(typeOf[Option[Seq[Int]]])(optSeqType)
    expectResult(typeOf[Seq[Int]])(optSeqType.asInstanceOf[TypeRef].args(0))

  }
}

case class SomeOrdinaryCaseClass(intVar: Int,
                                 stringVar: String,
                                 list: Seq[Int]) {
  def this() = this(0, "", Seq(1, 2, 2))
}


case class CaseClassWithDefaults(intVar: Int = 1, stringVar: String, list: Seq[Int] = Seq(1, 2, 3))

case class OptionsTestCase(opt: Option[Int])

case class OptionOfSeqCaseClass(optString: Option[String], optseq: Option[Seq[Int]])
