package com.griddynamics.genesis.validation


import com.griddynamics.genesis.api.{Failure, Success, ExtendedResult}
import util.matching.Regex
import Validation._
import com.griddynamics.genesis.validation.FieldConstraints._
import javax.validation.ConstraintViolation

trait ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]): ExtendedResult[Any]
}

class NotEmptyValidator extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) =
    notEmpty(value, value, propName, errorMessage)
}

class LengthConfigValidator(min: Int = 0, max: Int = 128) extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) =
    mustSatisfyLengthConstraints(value, value, propName)(min, max)
}

class RegexValidator(regex: Option[String] = None) extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) =
    mustMatch(value, propName, errorMessage)(new Regex(regex.getOrElse(params("name").toString)))(value)
}

class IntValidator(min: Int = Int.MinValue, max: Int = Int.MaxValue) extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) = try {
    val i = value.toInt
    if (i >= min && i <= max) Success(value) else Failure(variablesErrors = Map(propName -> errorMessage))
  } catch {
    case nfe: NumberFormatException => Failure(variablesErrors = Map(propName -> errorMessage))
  }
}

import collection.JavaConversions.asScalaSet

abstract class BeanConfigValidator[T](validator: javax.validation.Validator) extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) =
    validator.validate(getBean(value)).toSeq match {
      case Seq() => Success(value)
      case Seq(cv: ConstraintViolation[T], _*) => Failure(variablesErrors = Map(propName -> errorMessage))
  }
  def getBean(value: String): T
}

private[validation] case class email( @Email email: String)
class EmailConfigValidator(validator: javax.validation.Validator) extends BeanConfigValidator[email](validator) {
  def getBean(value: String) = email(value)
}

private[validation] case class url( @URL url: String)
class UrlConfigValidator(validator: javax.validation.Validator) extends BeanConfigValidator[url](validator) {
  def getBean(value: String) = url(value)
}