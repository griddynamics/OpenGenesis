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
package com.griddynamics.genesis.validation

import annotation.target.field
import javax.validation.{ConstraintValidatorContext, ConstraintValidator}
import org.springframework.beans.factory.annotation.Autowired
import org.hibernate.validator.constraints.impl.EmailValidator
import scala.collection


object FieldConstraints {
  import javax.validation.constraints
  import org.hibernate.validator.{constraints => hibernate}
  import com.griddynamics.genesis.{validation => custom}

  type Size = constraints.Size @field
  type Min = constraints.Min @field
  type Max = constraints.Max @field
  type Pattern = constraints.Pattern @field
  type Valid = javax.validation.Valid @field

  type NotBlank = hibernate.NotBlank @field
  type Email = hibernate.Email @field

  type ValidSeq = custom.ValidSeq @field
  type OptString = custom.OptString @field
  type OptEmail = custom.OptionalEmail @field
  type ValidStringMap = custom.ValidMap @field
}


class CustomSeqValidator extends ConstraintValidator[ValidSeq, scala.collection.Seq[_ <: AnyRef]] {
  @Autowired var validator: javax.validation.Validator = _

  def initialize(constraintAnnotation: ValidSeq) {}

  def isValid(value: scala.collection.Seq[_ <: AnyRef], context: ConstraintValidatorContext) = {
    import scala.collection.JavaConversions._
    var isValid = true
    context.disableDefaultConstraintViolation()

    for { (item, index) <- value.zipWithIndex } {
      val validateErrors = validator.validate(item)
      isValid = isValid && validateErrors.isEmpty

      for (error <- validateErrors) {
        val node = context.buildConstraintViolationWithTemplate(error.getMessage).addNode("[" + index + "]")
        val iterator = error.getPropertyPath.iterator()
        var property  = node.addNode(iterator.next().getName)

        for(propNode <- iterator) {
          property = property.addNode(propNode.getName)
        }

        property.addConstraintViolation()
      }
    }

    isValid
  }
}


class StringMapValidator extends ConstraintValidator[ValidMap, scala.collection.Map[String, String]]  {
  var keyMin: Int = 0
  var keyMax: Int = Int.MaxValue
  var valueMin: Int = 0
  var valueMax: Int = Int.MaxValue

  def initialize(constraintAnnotation: ValidMap) {
    keyMin = constraintAnnotation.key_min()
    keyMax = constraintAnnotation.key_max()

    valueMin = constraintAnnotation.value_min()
    valueMax = constraintAnnotation.value_max()
  }

  def isValid(value: collection.Map[String, String], context: ConstraintValidatorContext) = {
    var isValid = true
    context.disableDefaultConstraintViolation()

    for ( ((key, value), index) <- value.zipWithIndex ) {
      if(key.trim.size > keyMax || key.trim.size < keyMin) {
        val node =  context.buildConstraintViolationWithTemplate("Key value should be between %d and %d size".format(keyMin, keyMax)).addNode("[%d]".format(index)).addNode("name")
        node.addConstraintViolation()
        isValid = false
      }
      if(value.trim.size > valueMax || value.trim.size < valueMin) {
        val node = context.buildConstraintViolationWithTemplate("Value should be between %d and %d size".format(valueMin, valueMax)).addNode("[%d]".format(index)).addNode("value")
        node.addConstraintViolation()
        isValid = false
      }
    }

    isValid
  }
}


class OptionStringValidator extends ConstraintValidator[OptString, Option[String]] {
  var min: Int = 0
  var max: Int = Int.MaxValue
  var notBlank: Boolean = false

  def initialize(optSize: OptString) {
    this.min = optSize.min()
    this.max = optSize.max()
    this.notBlank = optSize.notBlank()
    validateParameters()
  }

  def isValid(value: Option[String], context: ConstraintValidatorContext) = {
    value.map( s => s.length >= min && s.length <= max && (!notBlank || s.isEmpty ||s.trim.nonEmpty) ).getOrElse(true)
  }

  private def validateParameters() {
    if (min < 0) {
      throw new IllegalArgumentException("The min parameter cannot be negative.")
    }
    if (max < 0) {
      throw new IllegalArgumentException("The max parameter cannot be negative.")
    }
    if (max < min) {
      throw new IllegalArgumentException("The length cannot be negative.")
    }
  }
}


class OptionEmailValidator extends ConstraintValidator[OptionalEmail, Option[String]] {
  val validator = new EmailValidator()

  def initialize(optSize: OptionalEmail) {}

  def isValid(value: Option[String], context: ConstraintValidatorContext) = {
    value.map( validator.isValid(_, context) ).getOrElse(true)
  }
}

