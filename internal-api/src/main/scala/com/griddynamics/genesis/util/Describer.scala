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
package com.griddynamics.genesis.util

import collection.mutable

class Describer(description: String) {
  val Unspecified: String = "unspecified";

  private val params = new mutable.LinkedHashMap[String, String]

  def param(key: String, value: Option[String]): Describer = {
    params(key) = value.getOrElse(Unspecified);
    this
  }

  def param(key: String, value: String): Describer = {
    param(key, Some(value))
  }

  def param(key: String, value: Iterable[_]): Describer = {
    params(key) = toString(value)
    this
  }

  def param(key: String, values: scala.collection.Map[_, _]): Describer = {
    params(key) = toString(values)
    this
  }

  def describe: String = {
    if (params.isEmpty) {
      description
    } else {
      "%s %s".format(description, toString(params.toMap))
    }
  }


  private def toString(list: Iterable[_]):String =
    "[ " +
      ( if(list.isEmpty)
        Unspecified
      else
        list.map(toString(_)).mkString(", ") ) +
    " ]"

  private def toString(tuple: (Any, Any)): String = tuple._1.toString + " = " + toString(tuple._2);

  private def toString(objRef: Any):String = objRef match {
    case map: scala.collection.Map[_,_] => toString(map)
    case list: Iterable[_] => toString(list)
    case option: Option[_] => option.map { toString(_) }.getOrElse(Unspecified);
    case other => other.toString
  }

  private def toString(values: scala.collection.Map[_, _]): String =
    "{ " +
    (if (values.isEmpty)
      Unspecified
    else
      values.map(toString(_)).mkString(", ")) +
    " }"

}
