package com.griddynamics.genesis.logs

import java.sql.Timestamp

case class Log(stepId : Int, message: String, timestamp: Timestamp)

case class ActionBasedLog(actionUID: String, message: String, timestamp: Timestamp)
