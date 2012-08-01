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
            case f: Failure => f
            case _ => {
                block(value)
            }
        }
    }

    def validOnUpdate(value: C)(block: C => ExtendedResult[C]): ExtendedResult[C] = {
        validateUpdate(value) match {
            case f: Failure => f
            case _ => {
                block(value)
            }
        }
    }
    protected def validateUpdate(c: C): ExtendedResult[C]
    protected def validateCreation(c: C): ExtendedResult[C]
}

object Validation {
    val namePattern = """^([a-z0-9.\-_]{2,32})$""".r
    val projectNamePattern = """^([\p{L}0-9.\-_ ]{2,64})$""".r
    val personNamePattern = """^([\p{L} ]{2,64})$""".r
    val emailPattern = """^(?=.{7,64}$)[a-z0-9_][a-z0-9_.\-]+@([a-z0-9_\-]+\.)+[a-z]{2,5}$""".r

    val projectNameErrorMessage = "Invalid format. Use a combination of alphanumerics, " +
                                  "spaces, dots, hyphens and underscores. Length must be from 2 to 64"
    val personNameErrorMessage = "Invalid format. Use a combination of letters and spaces. " +
                           "Length must be from 2 to 64"
    val nameErrorMessage = "Invalid format. Use a combination of latin lowercase letters, numbers, " +
                                  "dots, hyphens and underscores. Length must be from 2 to 32"
    val emailErrorMessage = "Invalid format. Note that only lowercase letters are allowed. Length must be from 7 to 64."

    val validADUserName = "^[^%<>]{1,128}$"
    // TODO: remove "GROUP_" prefix
    val validADGroupName = "^[^%<>]{1,122}$" // "GROUP_" prefix is added to every group name
    val ADUserNameErrorMessage = "User name [%s] is not valid. <,>,%% - are not allowed." +
        " Must be non-empty string no more than 128 characters long."
    val ADGroupNameErrorMessage = "Group name [%s] is not valid. <,>,%% - are not allowed." +
        " Must be non-empty string no more than 122 characters long."

    def mustMatch[C](obj: C, fieldName: String, error : String = "Invalid format")(pattern: Regex)(value: String) : ExtendedResult[C] = {
        value match {
            case pattern(s) => Success(obj)
            case _ => Failure(variablesErrors = Map(fieldName -> error))
        }
    }

    def mustMatchPersonName[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] =
      mustMatch(obj, fieldName, personNameErrorMessage)(personNamePattern)(value)
    def mustMatchName[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] =
      mustMatch(obj, fieldName, nameErrorMessage)(namePattern)(value)
    def mustMatchProjectName[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] =
      mustMatch(obj, fieldName, projectNameErrorMessage)(projectNamePattern)(value)
    def mustMatchEmail[C](obj: C, value: String, fieldName: String) : ExtendedResult[C] =
      mustMatch(obj, fieldName, emailErrorMessage)(emailPattern)(value)

    def mustPresent[C](obj:C, value: Option[_], fieldName: String, error : String = "Must be present") : ExtendedResult[C] = {
        value match {
            case None => Failure(variablesErrors = Map(fieldName -> error))
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

    def mustExist[C](value: C, errorMessage: String = "Not found")(finder: C => Option[_]) = {
      finder(value) match {
        case None => Failure(isNotFound = true, compoundServiceErrors = Seq(errorMessage))
        case Some(_) => Success(value)
      }
    }

    def mustSatisfyLengthConstraints[C](obj: C, value: String, fieldName: String)(minLen: Int = 0, maxLen: Int) : ExtendedResult[C] = {
      if (value.length < minLen || value.length > maxLen)
        Failure(variablesErrors = Map(fieldName -> "Length must be from %d to %d".format(minLen, maxLen)))
      else
        Success(obj)
    }
}