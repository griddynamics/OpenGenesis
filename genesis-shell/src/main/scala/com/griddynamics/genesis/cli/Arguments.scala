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
 */ package com.griddynamics.genesis.cli

trait CliArguments {
  def description: String
  def verbose: Boolean = false
}

trait VariablesSupport {
  def vars: String
  def variables: Map[String, String] = vars.split(";").collect{ case s: String if s.matches("^.+=.+$") => val kv=s.split("="); (kv(0).trim, kv(1).trim)}.toMap }

case class CreateArguments  (
    @GenesisShell.Param(name = "--template", usage = "template to be used to create env. Format: <template_name>/<version> ", required = true)
    template: String = "",
    @GenesisShell.Param(name = "--name", usage = "instance name", required = true)
    name: String = "",
    @GenesisShell.Param(name = "--variables", usage = "workflow parameters. Format '<name1>=<value1>;<name2>=<value2>;'", required = false)
    vars: String = "",
    @GenesisShell.Param(name = "--verbose", usage = "Show verbose workflow execution log", required = false)
    override val verbose: Boolean = false)
  extends CliArguments
  with VariablesSupport {

  def description = "Create new instance in project based on specified template"

  def templateName = template.split('/')(0)

  def templateVersion = template.split('/')(1)
}

case class DestroyArguments (
    @GenesisShell.Param(name = "--name", usage = "instance name", required = true)
    name: String = "",
    @GenesisShell.Param(name = "--verbose", usage = "Show verbose workflow execution log", required = false)
    override val verbose: Boolean = false)
  extends CliArguments {

  def description = "Destroys instance (runs destroy workflow)"
}

case class ExecuteArguments (
    @GenesisShell.Param(name = "--name", usage = "instance name", required = true)
    name: String = "",
    @GenesisShell.Param(name = "--workflow", usage = "name of workflow to be executed", required = true)
    workflow: String = "",
    @GenesisShell.Param(name = "--variables", usage = "workflow parameters. Format '<name1>=<value1>;<name2>=<value2>;'", required = false)
    vars: String = "",
    @GenesisShell.Param(name = "--verbose", usage = "Show verbose workflow execution log", required = false)
    override val verbose: Boolean = false)
  extends CliArguments
  with VariablesSupport {

  def description = "Executes workflow on specified instance"
}

case object ListEnvs extends CliArguments {

  def description = "Provides lists of active instances"
}

case object ListTemplates extends CliArguments {

  def description = "Provides list of accessible templates"
}

case class DescribeArguments(
    @GenesisShell.Param(name = "--name", usage = "instance name", required = true)
    name: String = ""
  ) extends CliArguments {

  def description = "Provides detailed instance information"
}