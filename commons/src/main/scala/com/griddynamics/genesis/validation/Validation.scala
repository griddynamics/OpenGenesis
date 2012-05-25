package com.griddynamics.genesis.validation

import util.matching.Regex
import com.griddynamics.genesis.api.{Success, ExtendedResult, Failure}

trait Validation[C] {

    def validCreate(value: C, function: (C) => C): ExtendedResult[C] = {
        validOnCreation(value) { item =>
            Success(function(item))
        }
    }


    def validUpdate(value: C, function: (C) => C): ExtendedResult[C] = {
        validOnUpdate(value) { item =>
            Success(function(item))
        }
    }

    def validOnCreation(value: C)(block: C => ExtendedResult[C]): ExtendedResult[C] = {
        validateCreation(value) match {
            case f: Failure[C] => f
            case _ => {
                block(value)
            }
        }
    }

    def validOnUpdate(value: C)(block: C => ExtendedResult[C]): ExtendedResult[C] = {
        validateUpdate(value) match {
            case f: Failure[C] => f
            case _ => {
                block(value)
            }
        }
    }
    protected def validateUpdate(c: C): ExtendedResult[C]
    protected def validateCreation(c: C): ExtendedResult[C]
}

object Validation {
    val usernamePattern = """^([a-zA-Z0-9@.-]{2,64})$""".r
    val projectNamePattern = """^([\p{L}0-9@.\-/_ ]{2,64})$""".r
    val namePattern = """^([\p{L} ]{2,128})$""".r
    val emailPattern = """^[\w][\w.-]+@([\w-]+\.)+[a-zA-Z]{2,5}$""".r

    val projectNameErrorMessage = "Invalid format. Use a combination of letters, numbers, " +
                                  "spaces and following symbols: @.-/_. Length must be from 2 to 64"
    val nameErrorMessage = "Invalid format. Use a combination of capital and lowercase letters and spaces. " +
                           "Length must be from 2 to 128"

    def mustMatch[C](obj: C, fieldName: String, error : String = "Invalid format")(pattern: Regex)(value: String) : ExtendedResult[C] = {
        value match {
            case pattern(s) => Success(obj)
            case _ => Failure[C](variablesErrors = Map(fieldName -> error))
        }
    }

    def mustMatchName[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] = mustMatch(obj, fieldName)(namePattern)(value)
    def mustMatchUserName[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] = mustMatch(obj, fieldName)(usernamePattern)(value)
    def mustMatchEmail[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] = mustMatch(obj, fieldName)(emailPattern)(value)

    def mustPresent[C](obj:C, value: Option[_], fieldName: String, error : String = "Must be present") : ExtendedResult[C] = {
        value match {
            case None => Failure[C](variablesErrors = Map(fieldName -> error))
            case _ => Success(obj)
        }
    }

    def notEmpty[C](obj: C, value: String, fieldName: String, error : String = "Must be present") : ExtendedResult[C] = {
        if (value == null || value.trim.length == 0) Failure(variablesErrors = Map(fieldName -> error))
        else Success(obj)
    }

    def must[C](value: C, errorMessage: String = "")(block: C => Boolean) : ExtendedResult[C] = {
        block(value) match {
            case true => Success(value)
            case false => Failure(compoundServiceErrors = Seq(errorMessage))
        }
    }

    def mustExist[C](value: C, errorMessage: String = "Not found")(finder: C => Option[C]) = {
      finder(value) match {
        case None => Failure(isNotFound = true, compoundServiceErrors = Seq(errorMessage))
        case Some(_) => Success(value)
      }
    }

}