package com.griddynamics.genesis.validation


import com.griddynamics.genesis.api.{Failure, Success, ExtendedResult}
import util.matching.Regex
import Validation._

trait ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]): ExtendedResult[Any]
}

class NotEmptyValidator extends ConfigValueValidator {
  def validate(propName: String, value: String, errorMessage: String, params: Map[String, Any]) =
    notEmpty(value, value, propName, errorMessage)
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