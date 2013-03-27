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

import com.griddynamics.genesis.workflow.Step
import groovy.lang.{MissingPropertyException, GroovyObjectSupport}
import com.griddynamics.genesis.plugin.StepBuilder
import com.griddynamics.genesis.util.ScalaReflectionUtils
import scala.collection.mutable
import scala.Some
import scala.reflect.runtime.universe.{Type, TypeTag, TypeRef, runtimeMirror, typeOf}

class ReflectionBasedStepBuilder(tpe: Type) extends GroovyObjectSupport with StepBuilder {
  import ReflectionBasedStepBuilder._

  val properties: mutable.Map[String, Any] = new mutable.HashMap()

  override def setProperty(property: String, newValue: Any) {
    try {
      super.setProperty(property, newValue)
    } catch {
      case e: MissingPropertyException => properties(property) = newValue
    }
  }

  def getDetails = {
    val (_, expectedParams) = ScalaReflectionUtils.getPrimaryConstructor(tpe)

    val (withValues, withoutValues) = expectedParams.partition {case (name, ttype) => properties.isDefinedAt(name) }
    val realValues = withValues.map {
      case (name, ttype) =>  (name, ScalaGroovyInterop.convert(properties(name), ttype) )
    }
    val validEmpties = withoutValues.collect {
      case (name, ttype: TypeRef) if hasEmptyValue(ttype) => (name, emptyValue(ttype))
    }
    ScalaReflectionUtils.newInstance(tpe, realValues.toMap, validEmpties.toMap).asInstanceOf[Step]
  }
}

object ReflectionBasedStepBuilder {

  val emptyValues = Map(
    typeOf[Option[_]] -> None,
    typeOf[Seq[_]] -> Seq(),
    typeOf[Set[_]] -> Set(),
    typeOf[List[_]] -> List(),
    typeOf[Map[_, _]] -> Map()
  )

  def hasEmptyValue(ttype: TypeRef): Boolean = emptyValues.keys.exists(ttype <:< _)

  def emptyValue(clazz: TypeRef): AnyRef = emptyValues.find {
    case (key, _) => clazz <:< key
  }.get._2

}

object ScalaGroovyInterop {

  private lazy val mirrorRT = runtimeMirror(getClass.getClassLoader)

  import scala.collection.JavaConversions._
  import com.griddynamics.genesis.util.ScalaUtils._

  def convert(value: Any, expectedType: Type) = (value, expectedType) match {
    case (x: java.util.List[_], ttype: TypeRef) => convertJavaList(x, ttype)
    case (x: java.util.Map[_, _], ttype: TypeRef) => convertJavaMap(x, ttype)
    case (x, ttype: TypeRef) if isScalaOption(ttype) => convertToOption(x, ttype)
    case (x, ttype: TypeRef) if x == null => null
    case (x, ttype: TypeRef) if (ttype <:< scalaType(x)) => x
    case (_, _) => throw new IllegalArgumentException(
      s"Failed to convert value $value to type ${expectedType.typeSymbol.name}: This kind of conversion is not supported")
  }

  private def scalaType(v: Any) = mirrorRT.classSymbol(getScalaClass(v)).toType

  private def convertToOption(x: Any, ttype: TypeRef) = if (x == null || ttype.args.forall {
    case typeArg: TypeRef => typeArg <:< scalaType(x)
  }) Option(x)
  else throw new IllegalArgumentException(
    s"Failed to convert value $x: doesn't conform Option[${ttype.args(0).typeSymbol.name}]")


  private def convertJavaMap(x: java.util.Map[_, _], ttype: TypeRef): AnyRef = {
    val map = mapAsScalaMap(x)
    val keyType = ttype.args(0)
    val valueType = ttype.args(1)
    val badTypeElements = map.filterNot {
      kv: (Any, Any) => keyType <:< scalaType(kv._1) && valueType <:< scalaType(kv._2)
    }
    if (badTypeElements.isEmpty) ttype match {
        case x: TypeRef if x <:< typeOf[Map[_, _]] => map.toMap
        case x: TypeRef if x <:< typeOf[java.util.Map[_, _]] => x
        case _ => throw new IllegalArgumentException(
          s"Failed to convert java.util.Map to ${ttype.typeSymbol.name}: this kind of target collection is not supported")
    } else throw new IllegalArgumentException(
    s"Elements [${badTypeElements.toString()}] in java.util.List do not conform to type Map[${keyType.typeSymbol.name}, ${valueType.typeSymbol.name}]")
  }

  def convertJavaList(x: java.util.List[_], ttype: TypeRef): AnyRef = {
    if (isScalaOption(ttype)) {
      ttype.args(0) match {
        case ttype0: TypeRef => return Some(convertJavaList(x, ttype0))
        case t => throw new IllegalArgumentException(s"Failed to convert java.util.List to Option[${t.typeSymbol.name}]")
      }
    }
    val elemType = ttype.args(0)
    val list = iterableAsScalaIterable(x)
    val badTypeElements = list.filterNot {elemType <:< scalaType(_)}
    if (badTypeElements.isEmpty) ttype match {
      case x if x <:< typeOf[Seq[_]] => list.toSeq
      case x if x <:< typeOf[IndexedSeq[_]] => list.toIndexedSeq
      case x if x <:< typeOf[Set[_]] => list.toSet
      case x if x <:< typeOf[List[_]] => list.toList
      case x if x <:< typeOf[java.util.List[_]] => x
      case _ => throw new IllegalArgumentException(
        s"Failed to convert java.util.List to ${ttype.typeSymbol.name}: this kind of target collection is not supported")
    } else {
      throw new IllegalArgumentException(s"Elements [${badTypeElements.toString()}] in java.util.List do not conform to type ${elemType.typeSymbol.name}")
    }
  }

  private[this] def isScalaOption(ttype: TypeRef) = ttype <:< typeOf[Option[_]]

}
