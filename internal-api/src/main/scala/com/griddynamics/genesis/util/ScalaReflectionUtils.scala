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

import java.lang.reflect.{ParameterizedType, Type, Constructor}
import com.thoughtworks.paranamer.{BytecodeReadingParanamer, CachingParanamer}
import tools.scalap.scalax.rules.scalasig.TypeRefType
import tools.scalap.scalax.rules.scalasig.{Type => ScalaType}
import scala.Some


//this class will be mostly outdated after scala 2.10 will be released
object ScalaReflectionUtils {

  private val byAmountOfParameters = new Ordering[Constructor[_]] {
    def compare(x: Constructor[_], y: Constructor[_]) = {
      x.getParameterTypes.size.compare(y.getParameterTypes.size)
    }
  }

  private val paranamer = new CachingParanamer(new BytecodeReadingParanamer)

  private val Name = """^((?:[^$]|[$][^0-9]+)+)([$][0-9]+)?$""".r

  implicit def class2companion(clazz: Class[_]) = new {
    def companionClass(classLoader: ClassLoader): Class[_] = {
      val path = if (clazz.getName.endsWith("$")) clazz.getName else "%s$".format(clazz.getName)
      Some(Class.forName(path, true, classLoader)).getOrElse(
        throw new Error("Could not resolve clazz='%s'".format(path))
      )
    }

    def companionObject(classLoader: ClassLoader) = companionClass(classLoader).getField("MODULE$").get(null)
  }


  private[this] def clean(name: String) = name match {
    case Name(text, junk) => text
  }

  def getPrimaryConstructor[T](clazz: Class[T]): (Constructor[T], List[(String, Type)]) = {
    val primary = rawClassOf(clazz).getDeclaredConstructors.max(byAmountOfParameters)

    val names = paranamer.lookupParameterNames(primary).map(clean)
    val loader = clazz.getClassLoader
    lazy val parsedSigTypes = ScalaCaseClassSigParser.parse(clazz, loader).toIndexedSeq

    val types = primary.getGenericParameterTypes.toList.zipWithIndex.map { case (constParam, constParamidx) =>
      constParam match {
        case parameterized: ParameterizedType => {
          val typeArgs = parameterized.getActualTypeArguments.toList.zipWithIndex.map {
            case (t, typeArgIdx) =>
              if (t == classOf[java.lang.Object]) {
                val paramTypeRef = parsedSigTypes.apply(constParamidx).apply(typeArgIdx).asInstanceOf[TypeRefType]
                ScalaUtils.loadClass(paramTypeRef.symbol.toString, clazz.getClassLoader)
              } else if(t.isInstanceOf[ParameterizedType]){
                val paramTypeRef = parsedSigTypes.apply(constParamidx).apply(typeArgIdx).asInstanceOf[TypeRefType]
                val intType = ScalaUtils.loadClass(paramTypeRef.symbol.toString, clazz.getClassLoader)
                mkParameterizedType(intType, paramTypeRef.typeArgs.map{ t => ScalaUtils.loadClass(t.asInstanceOf[TypeRefType].symbol.toString, clazz.getClassLoader)})
              } else {
                t
              }
          }
          mkParameterizedType(parameterized.getRawType, typeArgs)
        }
        case clazz: Class[_] => clazz
        case c => throw new IllegalStateException("Failed to recognize consturctor argument type %s for class %s".format(c, clazz.getCanonicalName))
      }
    }
    (primary.asInstanceOf[Constructor[T]], names.toList.zip(types))
  }


  private[this] def mkParameterizedType(owner: Type, typeArgs: Seq[Type]) =
    new ParameterizedType {
      def getActualTypeArguments = typeArgs.toArray

      def getOwnerType = owner

      def getRawType = owner

      override def toString = "Parameterized type { raw type = %s, type arguments = [%s] }".format(owner.toString, typeArgs.toString())
    }

  /**
   * WARNING: current implementation do not checks for types (especially when it comes to generics)
   */
  def newInstance[T](clazz: Class[T], values: Map[String, Any], defaults: Map[String, Any] = Map()): T = {
    val (const, params) = ScalaReflectionUtils.getPrimaryConstructor(clazz)
    val constParamNames = params.map(_._1)
    val allValues = if (!constParamNames.sameElements(values.keys)) {
      defaults ++ constructorDefaultValues(clazz, constParamNames) ++ values
    } else {
      values
    }
    const.newInstance(params.map {
      case (name, ttype) => allValues(name)
    }.toSeq.asInstanceOf[Seq[AnyRef]]: _*)
  }

  private[this] def rawClassOf(t: Type): Class[_] = t match {
    case c: Class[_] => c
    case p: ParameterizedType => rawClassOf(p.getRawType)
    case x => throw new IllegalArgumentException("Failed to get raw class value from type %s. This type is not supported".format(t.getClass.getCanonicalName))
  }


  def constructorDefaultValues(clazz: Class[_], constParams: List[(String)]): Map[String, Any] = {
    val companionClass: Class[_] = clazz.companionClass(clazz.getClassLoader)
    val companionObject: AnyRef = clazz.companionObject(clazz.getClassLoader)

    val defaults = constParams.zipWithIndex.map {
      case (name, index) =>
        val value: Option[Any] = try {
          Option(companionClass.getMethod("apply$default$%d".format(index + 1)).invoke(companionObject)) //this looks really fragile
        } catch {
          case _: Throwable => None
        }
        value.map((name, _))
    }.flatten

    defaults.toMap
  }
}



