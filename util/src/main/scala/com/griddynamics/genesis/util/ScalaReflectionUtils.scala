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

import scala.reflect.runtime.universe.{Type, TypeTag, typeOf, weakTypeOf, runtimeMirror, nme}

object ScalaReflectionUtils {

  private lazy val m = runtimeMirror(getClass.getClassLoader)

  private def getPrimaryConsSym(typ: Type) = {
    val constSym = typ.declaration(nme.CONSTRUCTOR)
    constSym.asTerm.alternatives.collect {
      case s if s.isMethod => s.asMethod
    }.find(_.isPrimaryConstructor).get
  }

  def getPrimaryConstructor(tpe: Type) = {
    val ctor = getPrimaryConsSym(tpe)
    val cm = m.reflectClass(tpe.typeSymbol.asClass)
    val ctorm = cm.reflectConstructor(ctor)
    (ctorm, ctor.paramss.flatten.map(s => (s.name.toString, s.typeSignature)))
  }

  /**
   * WARNING: current implementation do not checks for types (especially when it comes to generics)
   */
  def newInstance(tpe: Type, values: Map[String, Any], defaults: Map[String, Any] = Map()): Any = {
    val (const, params) = ScalaReflectionUtils.getPrimaryConstructor(tpe)
    val constParamNames = params.map(_._1)
    val allValues = if (!constParamNames.sameElements(values.keys)) {
      defaults ++ constructorDefaultValues(tpe) ++ values
    } else {
      values
    }
    const(params.map {
      case (name, ttype) => allValues(name)
    }.toSeq.asInstanceOf[Seq[AnyRef]]: _*)
  }

  def constructorDefaultValues(tpe: Type): Map[String, Any] = {
    val compSymbol = m.classSymbol(m.runtimeClass(tpe)).companionSymbol
    val compMirrorInst = m.reflectModule(compSymbol.asModule).instance
    val companionClass: Class[_] = compMirrorInst.getClass
    val companionObject: Any = compMirrorInst

    val ctorm = getPrimaryConsSym(tpe)
    val constParams = ctorm.paramss.flatten
    val defaults = constParams.zipWithIndex.map {
      case (paramSym, index)  if (paramSym.asTerm.isParamWithDefault) =>
        val value: Option[Any] = try {
          // have to do this until https://issues.scala-lang.org/browse/SI-6468 is fixed
          // in 2.10 'Reflection doesn't provide a way to find out values of default arguments'
          Option(companionClass.getMethod("apply$default$%d".format(index + 1)).invoke(companionObject)) //this looks really fragile
        } catch {
          case _: Throwable => None
        }
        value.map((paramSym.name.toString, _))
      case _ => None
    }.flatten

    defaults.toMap
  }
}



