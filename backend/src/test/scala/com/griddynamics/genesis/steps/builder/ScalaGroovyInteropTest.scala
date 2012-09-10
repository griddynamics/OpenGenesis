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
package com.griddynamics.genesis.steps.builder

import org.junit.Test
import java.util.Collections
import java.lang.reflect.ParameterizedType

class ScalaGroovyInteropTest {

  @Test def shouldConvertPrimitive() {
    val value = ScalaGroovyInterop.convert(1, classOf[Int])
    assert(value == 1)
  }

  @Test def shouldConvertBoxedToPrimitive() {
    val value = ScalaGroovyInterop.convert(new Integer(1), classOf[Int])
    assert(value == 1)
  }

  @Test def shouldConvertJListToScalaList() {
    val value = ScalaGroovyInterop.convert(Collections.singletonList(5), mkType(classOf[List[_]], Array(classOf[Int])))
    assert(value.isInstanceOf[List[_]] && value.asInstanceOf[List[_]].sameElements(List(5)))
  }

  @Test def shouldConvertJListToScalaSet() {
    val value = ScalaGroovyInterop.convert(Collections.singletonList(new Integer(5)), mkType(classOf[Set[_]], Array(classOf[Int])))
    assert(value.isInstanceOf[Set[_]] && value.asInstanceOf[Set[_]].sameElements(List(5)))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def convertToListShouldFailIfParameterTypeIsWrong() {
    ScalaGroovyInterop.convert(Collections.singletonList(new Integer(5)), mkType(classOf[Set[_]], Array(classOf[String])))
  }

  @Test def shouldConvertJMapToScalaMap() {
    val value = ScalaGroovyInterop.convert(Collections.singletonMap("Key", 666), mkType(classOf[Map[_, _]], Array(classOf[String], classOf[Int])))
    assert(value.asInstanceOf[Map[String, Int]].apply("Key") == 666)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def mapConvertShouldFailIfKeyTypesAreWrong() {
    ScalaGroovyInterop.convert(Collections.singletonMap("Not a number", 666), mkType(classOf[Map[_, _]], Array(classOf[Int], classOf[Int])))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def mapConvertShouldFailIfValueTypesAreWrong() {
    ScalaGroovyInterop.convert(Collections.singletonMap("Key", "Not a number"), mkType(classOf[Map[_, _]], Array(classOf[String], classOf[Int])))
  }

  @Test def nullShouldBeConvertedToNone() {
    val value = ScalaGroovyInterop.convert(null, mkType(classOf[Option[_]], Array(classOf[String])))
    assert(value == None)
  }

  @Test def valueShouldBeConvertedToSome() {
    val value = ScalaGroovyInterop.convert(5, mkType(classOf[Option[_]], Array(classOf[Int])))
    assert(value.asInstanceOf[Option[_]].get == 5)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def convertToOptionShouldRespectType() {
    ScalaGroovyInterop.convert("not a number", mkType(classOf[Option[_]], Array(classOf[Int])))
  }

  @Test def convertToOptionOfSeqOfInt() {
    val conversion = ScalaGroovyInterop.convert(Collections.singletonList(666), mkType(classOf[Option[_]], Array(mkType(classOf[Seq[_]], Array(classOf[Int])))))
    assert(conversion == Some(Seq(666)))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def convertToOptionOfSeqShouldRespectSecTypeParameter() {
    ScalaGroovyInterop.convert(Collections.singletonList("not a number"), mkType(classOf[Option[_]], Array(mkType(classOf[Seq[_]], Array(classOf[Int])))))
  }

  def mkType(rawType: java.lang.reflect.Type, argTypes: Array[java.lang.reflect.Type]): ParameterizedType = {
    new ParameterizedType {
      def getRawType = rawType

      def getActualTypeArguments = argTypes

      def getOwnerType = rawType
    }
  }
}
