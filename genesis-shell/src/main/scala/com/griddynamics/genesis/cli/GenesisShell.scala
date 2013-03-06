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

import com.griddynamics.genesis.api.{Configuration, Project, GenesisService}
import com.griddynamics.genesis.service.{StoreService, ProjectService, EnvironmentConfigurationService}
import java.util.Date
import org.kohsuke.args4j.{ExampleMode, CmdLineException, CmdLineParser}
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import scala.annotation.meta.field
import com.griddynamics.genesis.cli.commands.{WorkflowFailureException, DescribeCommand, ListInstancesCommand, ListTemplatesCommand, ExecuteCommand, DestroyCommand, CreateCommand}
import java.io.{PrintWriter, ByteArrayOutputStream, StringWriter}


object GenesisShell {
  type Param = org.kohsuke.args4j.Option @field

  var service: GenesisService = _
  var projectService: ProjectService = _
  var configurationService: EnvironmentConfigurationService = _
  var storeService: StoreService = _

  var defaultProjectId: Int = _
  var defaultConfiguration: Configuration = _
  val HeadTailQuotes = """^[\\"\\']*(.*?)[\\"\\']*$""".r

  def main(args: Array[String]) {
    if (!args.isEmpty) {
      println("OpenGenesis command line interface")
      execute(args) match {
        case Right(s) => println(s)
        case Left(s) => {
          System.err.println(s)
          System.exit(1)
        }
      }
    } else {
      runShell()
    }

    System.exit(0)
  }

  def runShell () {
    println("Starting OpenGenesis shell...")
    initialize
    println("Shell initialized. Type 'help' to get more information")
    Iterator.continually {
      Console.readLine()
    }.takeWhile(_.toLowerCase != "exit").foreach{ line =>
      val args = line.split("\\s+").map { p => val HeadTailQuotes(trimmed) = p; trimmed }
      execute(args) match {
        case Left(s) => System.err.println(s)
        case Right(s) => System.out.println(s)
      }
    }
  }

  def execute(args: Array[String]): Either[String, String] = {
    val cmd = if (args.head.toLowerCase == "help") args.drop(1) else args

    val command: CliArguments = cmd.headOption.getOrElse("").toLowerCase match {
      case "create" => new CreateArguments()
      case "destroy" => new DestroyArguments()
      case "execute" => new ExecuteArguments()
      case "describe" => new DescribeArguments()
      case "list-envs" =>  ListEnvs
      case "list-templates" =>  ListTemplates
      case "" =>
        return Right(showCLIHelp)
      case cmd =>
        return Left(s"Unknown command ${cmd}.\n$showCLIHelp")
    }

    val parser = new CmdLineParser(command)
    parser.setUsageWidth(120)

    if (cmd.size != args.size) {
      val baos = new ByteArrayOutputStream()
      parser.printUsage(baos)

      return Right(s"${command.description}\n${baos.toString}\n\nExample: ${parser.printExample(ExampleMode.ALL)}")
    }

    try {
      parser.parseArgument(args.drop(1): _ *)
    } catch {
      case e: CmdLineException =>
        val baos = new ByteArrayOutputStream()
        parser.printUsage(baos)
        val msg = s"Invalid arguments provided: ${e.getMessage}\n${baos.toString}"
        return Left(msg)
    }

    try {
      initialize

      command match {
        case c: CreateArguments => new CreateCommand(service).execute(c, defaultProjectId, defaultConfiguration)
        case c: DestroyArguments => new DestroyCommand(storeService, service).execute(c, defaultProjectId)
        case c: ExecuteArguments => new ExecuteCommand(storeService, service).execute(c, defaultProjectId)
        case c: DescribeArguments => new DescribeCommand(service, storeService).execute(c.name, defaultProjectId)
        case ListEnvs => new ListInstancesCommand(service).execute(defaultProjectId)
        case ListTemplates => new ListTemplatesCommand(service).execute(defaultProjectId)
      }
      Right("Command execution succeeded")
    } catch {
      case e: WorkflowFailureException =>
        Left("Command execution failed")
      case e: Exception =>
        val writer = new StringWriter()
        if (command.verbose) {
          e.printStackTrace(new PrintWriter(writer))
        }
        val errorMsg = s"[ERROR] ${e.getMessage} \n\n${writer.toString}"
        Left(errorMsg)
    }


  }

  def showCLIHelp: String = {
    """ Allowed commands:
        | - create
        | - destroy
        | - execute
        | - list-envs
        | - list-templates
        | - describe
        | - help <command-name>
      """.stripMargin
  }

  def cliProject(): Project = {
    val prj = projectService.list.find(_.name == "genesis-cli")

    val project = prj.getOrElse {
      projectService.create(new Project(id = None, name = "genesis-cli", creator = "genesis-cli", creationTime = new Date().getTime, projectManager = "genesis-cli", description = None)).get
    }
    project
  }

  lazy val initialize = {
    val profiles = Option(System.getProperty("spring.profiles.active"))
    profiles match {
      case Some(p) if !p.trim().isEmpty => System.setProperty("spring.profiles.active", p + ", genesis-cli")
      case _ => System.setProperty("spring.profiles.active", "genesis-cli")
    }

    val springConfig = "classpath:/WEB-INF/spring/backend-config.xml"
    val appContext = new ClassPathXmlApplicationContext(springConfig)

    SecurityContextHolder.getContext.setAuthentication(new UsernamePasswordAuthenticationToken("genesis", "genesis"))

    this.service = appContext.getBean(classOf[GenesisService])
    this.projectService = appContext.getBean(classOf[ProjectService])
    this.configurationService = appContext.getBean(classOf[EnvironmentConfigurationService])
    this.storeService = appContext.getBean(classOf[StoreService])

    val project = cliProject()
    this.defaultProjectId = project.id.get
    this.defaultConfiguration = configurationService.getDefault(this.defaultProjectId).get

    true
  }
}


