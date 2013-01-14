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

import tools.scalap.scalax.rules.scalasig._
import reflect.ScalaSignature
import scala.{Some, Error}
import tools.scalap.scalax.rules.scalasig.MethodSymbol
import tools.scalap.scalax.rules.scalasig.ClassFile
import tools.scalap.scalax.rules.scalasig.ClassSymbol
import tools.scalap.scalax.rules.scalasig.ScalaSig
import tools.scalap.scalax.rules.scalasig.TypeRefType
import reflect.internal.pickling.ByteCodecs

object ScalaCaseClassSigParser {

  def parse[A](clazz: Class[A], classLoader: ClassLoader): Seq[Seq[Type]] = findSym(clazz, classLoader)
    .children
    .collect { case c: MethodSymbol if c.isCaseAccessor && !c.isPrivate => c.infoType }
    .map {
    _ match {
      case NullaryMethodType(t: TypeRefType) => t.typeArgs
      case _ => Nil
    }
  }

  protected def findSym[A](clazz: Class[A], classLoader: ClassLoader): ClassSymbol = {
    val simpleName = clazz.getName.split("\\$").last
    val pss = parseScalaSig(clazz, classLoader).getOrElse {
      throw new Error("Failed to parse Scala signature from: %s".format(clazz))
    }

    pss.topLevelClasses.headOption.getOrElse {
      val topLevelObjects = pss.topLevelObjects
      topLevelObjects.headOption match {
        case Some(tlo) => {
          pss.symbols.find {
            s => !s.isModule && s.name == simpleName
          }.getOrElse {
            throw new Error("Parsed Scala signature, but no expected module with name = %s found: %s".format(simpleName, clazz))
          }.asInstanceOf[ClassSymbol]
        }
        case _ => throw new Error("Parsed scala signature, but no expected type found: %s".format(clazz))
      }
    }
  }

  private def parseScalaSig(_clazz: Class[_], classLoader: ClassLoader): Option[ScalaSig] = {
    val clazz = findRootClass(_clazz, classLoader)
    parseClassFileFromByteCode(clazz).map(ScalaSigParser.parse(_)).getOrElse(None) orElse
      parseByteCodeFromAnnotation(clazz).map(ScalaSigAttributeParsers.parse(_)) orElse
      None
  }

  protected def findRootClass(klass: Class[_], classLoader: ClassLoader) = {
    ScalaUtils.loadClass(klass.getName.split("\\$").head, classLoader)
  }


  private def parseClassFileFromByteCode(clazz: Class[_]): Option[ClassFile] = try {
    Option(ClassFileParser.parse(ByteCode.forClass(clazz)))
  } catch {
    case e: NullPointerException => None
  }

  private def parseByteCodeFromAnnotation(clazz: Class[_]): Option[ByteCode] = {
    if (clazz.isAnnotationPresent(classOf[ScalaSignature])) {
      val sig = clazz.getAnnotation(classOf[ScalaSignature])
      val bytes = sig.bytes.getBytes("UTF-8")
      val len = ByteCodecs.decode(bytes)
      Option(ByteCode(bytes.take(len)))
    } else {
      None
    }
  }
}
