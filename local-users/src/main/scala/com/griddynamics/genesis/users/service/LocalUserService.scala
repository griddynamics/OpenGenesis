/*
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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.users.service

import com.griddynamics.genesis.users.UserService
import com.griddynamics.genesis.users.repository.LocalUserRepository
import org.springframework.transaction.annotation.{Propagation, Transactional}
import com.griddynamics.genesis.api.{RequestResult, User}
import collection.Seq
import util.matching.Regex

class LocalUserService(val repository: LocalUserRepository) extends UserService {


    @Transactional(readOnly = true)
    def findByUsername(username: String) = {
        repository.findByUsername(username)
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def create(user: User) : RequestResult = {
        validOnCreation(user) {
            user => repository.insert(user)
        }
    }
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    override def update(user: User) : RequestResult = {
         validOnUpdate(user) {
             user => {
                 repository.findByUsername(user.username) match {
                     case None => RequestResult(isSuccess = false, compoundServiceErrors = Seq("Not found"))
                     case Some(lu) => {
                         repository.update(user)
                         RequestResult(isSuccess = true)
                     }
                 }
             }
         }
    }

    def validOnCreation[B](user: User)(block : User => B ) : RequestResult = {
        validateCreation(user) match{
            case Some(rr) => rr
            case _ => {
                block(user)
                RequestResult(isSuccess = true)
            }
        }
    }

    def validOnUpdate(user: User)(block: User => RequestResult) : RequestResult = {
        validateUpdate(user) match{
            case Some(rr) => rr
            case _ => {
                block(user) ++ RequestResult(isSuccess = true)
            }
        }
    }

    private[service] def validateUpdate(user: User) = {
        import LocalUserService._
        val results = Seq(
            mustMatch(user.firstName, namePattern, "firstName"),
            mustMatch(user.lastName, namePattern, "lastName"),
            mustMatch(user.email, emailPattern, "email")
        ).filter(_.isDefined).map(_.get)
        results.isEmpty match {
            case true => None
            case _ => Some(results.reduceLeft(_ ++ _))
        }
    }

    private[service] def validateCreation(user: User) = {
        import LocalUserService._
        val results = Seq(
            must(user){
                user => repository.findByUsername(user.username).isEmpty
            },
            mustMatch(user.username, usernamePattern, "username"),
            mustMatch(user.firstName, namePattern, "firstName"),
            mustMatch(user.lastName, namePattern, "lastName"),
            mustMatch(user.email, emailPattern, "email"),
            mustPresent(user.password, "password")
        ).filter(_.isDefined).map(_.get)
        results.isEmpty match {
            case true => None
            case _ => Some(results.reduceLeft(_ ++ _))
        }
    }

    @Transactional(readOnly = true)
    def all = {
       repository.list
    }
}

object LocalUserService {
    val usernamePattern = """^([a-zA-Z0-9@.-]{2,64})$""".r
    val namePattern = """^([a-zA-Z ]{2,128})$""".r
    val emailPattern = """^[\w][\w.-]+@([\w-]+\.)+[a-zA-Z]{2,5}$""".r

    def mustMatch(value: String, pattern: Regex, fieldName: String, error : String = "Invalid format") = {
        value match {
            case pattern(s) => None
            case _ => Some(RequestResult(variablesErrors = Map(fieldName -> error), isSuccess = false))
        }
    }

    def mustPresent(value: Option[String], fieldName: String, error : String = "Must be present") = {
        value match {
            case None => Some(RequestResult(variablesErrors = Map(fieldName -> error), isSuccess = false))
            case _ => None
        }
    }

    def must(value: User)(block: User => Boolean, error: String = "") = {
        block(value) match {
            case true => None
            case false => Some(RequestResult(isSuccess = false, compoundVariablesErrors = Seq(error)))
        }
    }
}

