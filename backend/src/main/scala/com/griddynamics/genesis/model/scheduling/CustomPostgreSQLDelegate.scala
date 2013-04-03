package com.griddynamics.genesis.model.scheduling

import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
import org.quartz.spi.ClassLoadHelper
import org.slf4j.Logger
import java.sql.{PreparedStatement, SQLException, ResultSet}

/**
 * Overridden version of Quartz delegate for PostgreSQL.
 * Does special handling for boolean values. OpenGenesis
 * use varchar for them, but Postgres does not have automatic
 * conversion for it. Since we can't change Quartz SQL statements,
 * this class should handle it.
 */
class CustomPostgreSQLDelegate(log: Logger, tablePrefix: String, schedName: String,
                               instanceId: String, classLoadHelper: ClassLoadHelper, useProperties: Boolean)
  extends PostgreSQLDelegate(log, tablePrefix, schedName, instanceId, classLoadHelper, useProperties) {

  def this(log: Logger, tablePrefix: String, schedName: String, instanceId: String,
           classLoadHelper: ClassLoadHelper) = {
    this(log, tablePrefix, schedName, instanceId, classLoadHelper, false) }

  override def getBoolean(rs: ResultSet, columnName: String) : Boolean = {
    try {
      super.getBoolean(rs, columnName)
    } catch {
      case e: SQLException => "true".equalsIgnoreCase(rs.getString(columnName))
      case x => throw x
    }
  }

  override def getBoolean(rs: ResultSet, columnIndex: Int) : Boolean = {
    try {
      super.getBoolean(rs, columnIndex)
    } catch {
      case e: SQLException => "true".equalsIgnoreCase(rs.getString(columnIndex))
      case x => throw x
    }
  }

  override def setBoolean(ps: PreparedStatement, columnIndex: Int, v: Boolean) {
     ps.setString(columnIndex, v.toString.toLowerCase)
  }
}
