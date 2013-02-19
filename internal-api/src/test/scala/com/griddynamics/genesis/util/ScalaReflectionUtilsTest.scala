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
import java.lang.reflect.ParameterizedType

class ScalaReflectionUtilsTest {

  @Test def shouldFindPrimaryConstructor() {
    val (const, params) = ScalaReflectionUtils.getPrimaryConstructor(classOf[SomeOrdinaryCaseClass])
    assert(params.size == 3)
    val paramsMap = params.toMap

    assert(paramsMap("intVar") == classOf[Int])
    assert(paramsMap("stringVar") == classOf[String])

    val listType = paramsMap("list")
    assert(listType.isInstanceOf[ParameterizedType])
    assert(listType.asInstanceOf[ParameterizedType].getRawType == classOf[Seq[_]])
    assert(listType.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0) == classOf[java.lang.Integer])
  }

  @Test def newInstanceCreation() {
    val constParams: Map[String, Any] = Map("intVar" -> 1, "stringVar" -> "stringValue", "list" -> Seq(3, 2, 1))
    val instance = ScalaReflectionUtils.newInstance(classOf[SomeOrdinaryCaseClass], constParams)

    assert(instance == new SomeOrdinaryCaseClass(intVar = 1, stringVar = "stringValue", list = Seq(3, 2, 1)))
  }

  @Test(expected = classOf[Exception])
  @Ignore("type parameters check are not imlemented yet")
  def newInstanceShouldRespectTypeParameters() {
    val instance = ScalaReflectionUtils.newInstance(classOf[OptionsTestCase], Map("opt" -> Some("not a number")))
  }

  @Test def newInstanceShouldUseDefaultValues() {
    val instance = ScalaReflectionUtils.newInstance(classOf[CaseClassWithDefaults], Map("stringVar" -> "some"))
    assert(new CaseClassWithDefaults(stringVar = "some") == instance)
  }

  @Test def newInstanceShouldOverrideDefaultValuesIfProvided() {
    assert(classOf[Option[_]].isAssignableFrom(Some(0).getClass))
    val instance = ScalaReflectionUtils.newInstance(classOf[CaseClassWithDefaults], Map("stringVar" -> "some", "intVar" -> 666))
    assert(new CaseClassWithDefaults(intVar = 666, stringVar = "some") == instance)
  }

  @Test def collectDefaultValuesShouldReturnConsDefaultValues() {
    val (construct, params) = ScalaReflectionUtils.getPrimaryConstructor(classOf[CaseClassWithDefaults])
    val defaults = ScalaReflectionUtils.constructorDefaultValues(classOf[CaseClassWithDefaults], params.map(_._1))

    assert(defaults("intVar") == 1)
    assert(defaults("list").asInstanceOf[Seq[Int]].sameElements(Seq(1, 2, 3)))
    assert(defaults.get("stringVar").isEmpty)
  }

  @Test def optionOfSeqOfInts() {
    val (construct, params) = ScalaReflectionUtils.getPrimaryConstructor(classOf[OptionOfSeqCaseClass])
    val map = params.toMap

    val optStringType = map("optString")
    assert(optStringType.isInstanceOf[ParameterizedType])
    assert(optStringType.asInstanceOf[ParameterizedType].getRawType == classOf[Option[_]])
    assert(optStringType.asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0) == classOf[String])

    val optSeqType  = map("optseq").asInstanceOf[ParameterizedType]

    assert(optSeqType.getRawType == classOf[Option[_]])
    assert(optSeqType.getActualTypeArguments.apply(0).isInstanceOf[ParameterizedType])

    val seqInOptionType = optSeqType.getActualTypeArguments.apply(0).asInstanceOf[ParameterizedType]
    assert(seqInOptionType.getRawType == classOf[Seq[_]])
    assert(seqInOptionType.getActualTypeArguments.apply(0) == classOf[java.lang.Integer])
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
