/**
 *   Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
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
 *   Description: Continuous Delivery Platform
 */ package com.griddynamics.genesis.configuration

import com.yammer.metrics.core.HealthCheck
import java.lang.String
import java.sql.{Connection, ResultSet, Statement, SQLException}
import javax.sql.DataSource
import scala.util.control.Exception._
import org.springframework.context.annotation.{Profile, Configuration}
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.dbcp.BasicDataSource
import javax.annotation.PostConstruct
import com.yammer.metrics.HealthChecks

@Configuration
@Profile(Array("server"))
class DatabaseHealthCheckContext {

  @Autowired var dataSource: BasicDataSource  = _

  @PostConstruct
  def init() {
    HealthChecks.register(new DatabaseHealthCheck(dataSource))
  }
}

class DatabaseHealthCheck(dataSource: DataSource) extends HealthCheck("database health check") {
  def check() = {
    val query: String = "SELECT 1"

    var conn: Connection = null
    var stmt: Statement = null
    var rset: ResultSet = null
    try {
      conn = dataSource.getConnection
      stmt = conn.createStatement
      stmt.setQueryTimeout(2)
      rset = stmt.executeQuery(query)
      if (rset.next())
        HealthCheck.Result.healthy()
      else
        HealthCheck.Result.unhealthy("Ping message did not return response")
    } catch {
      case e: Exception => HealthCheck.Result.unhealthy(e)
    } finally {
      Option(rset).map( r => ignoring(classOf[Exception]) { r.close() })
      Option(stmt).map( s => ignoring(classOf[Exception]) { s.close() })
      Option(conn).map( c => ignoring(classOf[Exception]) { c.close() })
    }
  }
}