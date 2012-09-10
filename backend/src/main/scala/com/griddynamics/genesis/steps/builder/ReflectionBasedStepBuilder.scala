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
import java.lang.reflect.{ParameterizedType, Type}
import scala.collection.mutable

class ReflectionBasedStepBuilder(clazz: Class[_ <: Step]) extends GroovyObjectSupport with StepBuilder {
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
    val (_, expectedParams) = ScalaReflectionUtils.getPrimaryConstructor(clazz)

    val (withValues, withoutValues) = expectedParams.partition {case (name, ttype) => properties.isDefinedAt(name) }

    val realValues = withValues.map {case (name, ttype) => (name, ScalaGroovyInterop.convert(properties(name), ttype) ) }
    val validEmpties = withoutValues.collect {
      case (name, ttype: ParameterizedType) if hasEmptyValue(ttype.getRawType) =>  (name, emptyValue(ttype.getRawType))
      case (name, clazz: Class[_]) if hasEmptyValue(clazz) => (name, emptyValue(clazz))
    }

    ScalaReflectionUtils.newInstance(clazz, realValues.toMap, validEmpties.toMap)
  }
}

object ReflectionBasedStepBuilder {

  val emptyValues = Map(
    classOf[Option[_]] -> None,
    classOf[Seq[_]] -> Seq(),
    classOf[Set[_]] -> Set(),
    classOf[List[_]] -> List(),
    classOf[Map[_, _]] -> Map()
  )

  def hasEmptyValue(ttype: Type): Boolean = ttype.isInstanceOf[Class[_]] && hasEmptyValue(ttype.asInstanceOf[Class[_]])

  def hasEmptyValue(clazz: Class[_]): Boolean = emptyValues.keys.exists(clazz.isAssignableFrom(_))

  def emptyValue(clazz: Class[_]): AnyRef = emptyValues.find {
    case (key, _) => clazz.isAssignableFrom(key)
  }.get._2

  def emptyValue(ttype: Type): AnyRef = emptyValue(ttype.asInstanceOf[Class[_]])
}

object ScalaGroovyInterop {

  import scala.collection.JavaConversions._
  import com.griddynamics.genesis.util.ScalaUtils._

  def convert(value: Any, expectedType: Type) = (value, expectedType) match {
    case (x: java.util.List[_], ttype: ParameterizedType) => convertJavaList(ttype, x)
    case (x: java.util.Map[_, _], ttype: ParameterizedType) => convertJavaMap(x, ttype)
    case (x, ttype: ParameterizedType) if isScalaOption(ttype.getRawType) => convertToOption(x, ttype)
    case (x, ttype: Class[_]) if x == null => null
    case (x, ttype: Class[_]) if (getType(x) == ttype || ttype.isAssignableFrom(toAnyRef(x).getClass)) => x
    case (_, _) => throw new IllegalArgumentException("Failed to convert value %s to type %s: This kind of conversion is not supported".format(value, expectedType))
  }

  def convertToOption(x: Any, ttype: ParameterizedType) = {
    if (x == null || ttype.getActualTypeArguments.forall {
      case typeArg: Class[_] => typeArg.isAssignableFrom(getType(x)) || typeArg.isAssignableFrom(toAnyRef(x).getClass)
    })
      Option(x)
    else
      throw new IllegalArgumentException("Falied to convert value %s: doesn't conform Option[%s]".format(x, ttype.getActualTypeArguments.apply(0)))
  }

  def convertJavaMap(x: java.util.Map[_, _], ttype: ParameterizedType): AnyRef = {
    val map = mapAsScalaMap(x)
    val keyType = ttype.getActualTypeArguments.apply(0).asInstanceOf[Class[_]]
    val valueType = ttype.getActualTypeArguments.apply(1).asInstanceOf[Class[_]]
    val badTypeElements = map.filterNot {
      mv: (Any, Any) => keyType.isAssignableFrom(getType(mv._1)) && valueType.isAssignableFrom(getType(mv._2))
    }
    if (badTypeElements.isEmpty) {
      ttype.getRawType match {
        case x: Class[_] if x.isAssignableFrom(classOf[Map[_, _]]) => map.toMap
        case x: Class[_] if x.isAssignableFrom(classOf[java.util.Map[_, _]]) => x
        case _ => throw new IllegalArgumentException("Failed to convert java.util.Map to %s: this kind of target collection is not supported".format(ttype.getRawType))
      }
    } else {
      throw new IllegalArgumentException("Elements [%s] in java.util.List do not conform to type Map[%s, %s]".format(
        badTypeElements.toString(), keyType.getCanonicalName, valueType.getCanonicalName))
    }
  }

  def convertJavaList(ttype: ParameterizedType, x: java.util.List[_]): AnyRef = {
    if (isScalaOption(ttype.getRawType)) {
      ttype.getActualTypeArguments.apply(0) match {
        case ttype: ParameterizedType => return Some(convertJavaList(ttype, x))
        case t => throw new IllegalArgumentException("Failed to convert java.util.List to Option[%s]".format(t))
      }
    }
    val elemType = ttype.getActualTypeArguments.apply(0).asInstanceOf[Class[_]]
    val list = iterableAsScalaIterable(x)
    val badTypeElements = list.filterNot {
      item => elemType.isAssignableFrom(toAnyRef(item).getClass) || elemType.isAssignableFrom(getType(item))
    }
    if (badTypeElements.isEmpty) {
      ttype.getRawType match {
        case x: Class[_] if x.isAssignableFrom(classOf[Seq[_]]) => list.toSeq
        case x: Class[_] if x.isAssignableFrom(classOf[IndexedSeq[_]]) => list.toIndexedSeq
        case x: Class[_] if x.isAssignableFrom(classOf[Set[_]]) => list.toSet
        case x: Class[_] if x.isAssignableFrom(classOf[List[_]]) => list.toList
        case x: Class[_] if x.isAssignableFrom(classOf[java.util.List[_]]) => x
        case _ => throw new IllegalArgumentException("Failed to convert java.util.List to %s: this kind of target collection is not supported".format(ttype.getRawType))
      }
    } else {
      throw new IllegalArgumentException("Elements [%s] in java.util.List do not conform to type %s".format(badTypeElements.toString(), elemType.getCanonicalName))
    }
  }

  private[this] def isScalaOption(ttype: Type) = {
    ttype.isInstanceOf[Class[_]] && ttype.asInstanceOf[Class[_]].isAssignableFrom(classOf[Option[_]])
  }
}
