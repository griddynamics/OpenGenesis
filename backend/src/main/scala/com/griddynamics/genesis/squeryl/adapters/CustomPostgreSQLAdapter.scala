package com.griddynamics.genesis.squeryl.adapters

import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.{ReferentialAction, Schema, Session, Table}
import org.squeryl.internals.{FieldMetaData, StatementWriter}


/**
 * This class overrides default PostgreSQL adapter for two purposes:
 *
 * - For the spring-security-acl it adds defaults for tables that uses squeryl-oriented sequence names
 * - For the Quartz it removes quotes around tables.
 *
 * In Postgres quotes has somewhat special meaning.
 * If table created as <code>CREATE TABLE "TABLE_NAME"</code>,
 * then you should do a query like <code>SELECT * FROM "TABLE_NAME"</code>,
 * otherwise Postgres will report that there are no table <code>table_name</code>.
 * Unfortunately, Squeryl quotes all table/indexes/field names when creating it.
 * However, we can't (again) change Quartz queries.
 */
class CustomPostgreSQLAdapter extends PostgreSqlAdapter {
  override def quoteName(s: String) = s
  override def postCreateTable(t: Table[_], printSinkWhenWriteOnlyMode: Option[String => Unit]) = {

    val autoIncrementedFields = t.posoMetaData.fieldsMetaData.filter(_.isAutoIncremented)

    def executeWriter(sw: StatementWriter) {
      if (printSinkWhenWriteOnlyMode == None) {
        val st = Session.currentSession.connection.createStatement
        st.execute(sw.statement)
      }
      else
        printSinkWhenWriteOnlyMode.get.apply(sw.statement + ";")
    }
    for(fmd <-autoIncrementedFields) {
      val sw = new StatementWriter(false, this)
      sw.write("create sequence ", quoteName(fmd.sequenceName))
      executeWriter(sw)
      val alterWriter = new StatementWriter(false, this)
      alterWriter.write(s"alter table ${quoteName(t.prefixedName.toLowerCase)} alter column ${fmd.columnName} set default nextval('${fmd.sequenceName}'::regclass)")
      executeWriter(alterWriter)
    }
  }
}
