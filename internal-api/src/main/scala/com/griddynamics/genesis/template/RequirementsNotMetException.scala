package com.griddynamics.genesis.template

class RequirementsNotMetException(val messages: Seq[String]) extends RuntimeException {
    override def getMessage = messages.mkString("\n")
}
