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
 * Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.ad

import com4j.typelibs.ado20.{_Connection, _Recordset, ClassFactory, Fields}
import com4j.{ComException, Variant}
import collection.mutable.ListBuffer
import com.griddynamics.genesis.util.Logging
import javax.naming.ldap.LdapName

class CommandTemplate(pool: ActiveDirectoryConnectionPool) extends Logging {

  def query[T](queryCommand: Command, mapper: FieldsMapper[T]): Seq[T] = {
    val commandText = queryCommand.toText

    log.debug("Command: %s", commandText)

    var connection: _Connection = null
    val command = ClassFactory.createCommand

    try {
      connection = pool.acquire()

      command.activeConnection(connection)

      command.commandText(commandText)

      val rs: _Recordset = command.execute(null, Variant.getMissing, -1)

      val result = new ListBuffer[T]()

      try {

        while (!rs.eof) {
          result += mapper.mapFromFields(rs.fields())
          rs.moveNext()
        }

      } catch {
        case e: ComException if e.getHRESULT == ComErrors.SIZE_LIMIT =>
          log.warn("Size limit exceeds error occured", e)
      }

      log.debug("Found %s record(s)", result.size)

      result
    } finally {
      try {
        if (command != null) command.dispose()
      } finally {
        if (connection != null) pool.release(connection)
      }
    }
  }

}

class Command(namingContext: String, filter: String, attrsToReturn: String, searchScope: String) {
  def toText: String = s"<LDAP://$namingContext>;$filter;$attrsToReturn;$searchScope"
}

trait FieldsMapper[T] {
  def mapFromFields(fields: Fields): T
}

trait MappingUtils {
  protected def config: ActiveDirectoryPluginConfig

  protected def getStringField(field: String, fields: Fields): Option[String] = try {
    Option(fields.item(field).value()) map { _.toString }
  } catch {
    case e: ComException if e.getHRESULT == ComErrors.NON_REQUESTED_FIELD =>
      throw new IllegalArgumentException("Field %s was not requested".format(field))
  }

  protected def getDomain(fields: Fields): String = {
    val dnOpt = getStringField("distinguishedName", fields)

    val name = dnOpt map { new LdapName(_) } getOrElse {
      throw new IllegalArgumentException("AD attribute 'distinguishedName' is empty")
    }

    if (name.size() < 2)
      throw new IllegalArgumentException("Too short DN (%s) to extract domain".format(name))

    if (!name.get(1).matches("(?i)dc=\\w+"))
      throw new RuntimeException("Cannot get user domain. Unknown DN format (%s)".format(name))

    name.get(1).split('=')(1).toUpperCase
  }

  protected def getOptionalAccountName(fields: Fields): Option[String] = {
    val username = getStringField("sAMAccountName", fields)
    username.map { name =>
      if (config.useDomain) getDomain(fields) + "\\" + name else name
    }
  }

  protected def getAccountName(fields: Fields): String = {
    val username = getStringField("sAMAccountName", fields) getOrElse {
      throw new IllegalArgumentException("AD attribute 'sAMAccountName' is empty")
    }

    if (config.useDomain) getDomain(fields) + "\\" + username else username
  }
}

