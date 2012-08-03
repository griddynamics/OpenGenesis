package com.griddynamics.genesis.chef.executor

import ChefNodeInitializer._
import com.griddynamics.genesis.exec.ExecNodeInitializer._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST.{JObject, JField, JString, JArray}
import com.griddynamics.genesis.util.{JsonUtil, Logging}
import com.griddynamics.genesis.exec.ExecDetails
import com.griddynamics.genesis.service.SshService
import com.griddynamics.genesis.workflow.{Signal, SyncActionExecutor}
import com.griddynamics.genesis.util.shell.command.{chmod, mkdir}
import com.griddynamics.genesis.chef.action.{PrepareRegularChefRun, PrepareInitialChefRun, ChefRunPrepared, PrepareChefRun}
import com.griddynamics.genesis.chef.{ChefVmAttrs, ChefService}


trait RegularChefRun extends ChefRunPreparer[PrepareRegularChefRun] {
    def chefClientAttrs = {
        val runList = JArray(action.runList.map(JString(_)).toList)
        new JObject(JField("run_list", runList) +: action.jattrs.obj.filter(_.name != "run_list"))
    }
}

abstract class ChefRunPreparer[A <: PrepareChefRun](val action: A,
                                                    sshService: SshService,
                                                    val chefService: ChefService)
  extends SyncActionExecutor with Logging {

  val runDir = genesisDir / action.label

  val chefLog = runDir / "chef.log"
  val chefAttrs = runDir / "attrs.json"
  val chefConfig = runDir / "client.rb"

  val execDetails = new ExecDetails(action.env, action.server, runDir / "chef-run.sh", runDir)

  val chefLogLevel = ":info"

  lazy val sshClient = sshService.sshClient(action.env, action.server)

  def startSync() = {
    sshClient.exec(mkdir(runDir))

    sshClient.put(chefConfig, chefClientConfig)
    sshClient.put(chefAttrs, JsonUtil.toString(chefClientAttrs))

    sshClient.put(execDetails.execPath, chefRunShContent)
    sshClient.exec(chmod("+x", execDetails.execPath))

    ChefRunPrepared(action, execDetails)
  }

  def chefClientConfig =
    "log_level              %s                   \n".format(chefLogLevel) +
      "log_location           \"#{ENV['HOME']}/%s\"\n".format(chefLog) +
      "node_name              \"%s\"               \n".format(action.server(ChefVmAttrs.ChefNodeName)) +
      "client_key             \"#{ENV['HOME']}/%s\"\n".format(clientPem) +
      "validation_client_name \"%s\"               \n".format(chefService.validatorCredentials.identity) +
      "validation_key         \"#{ENV['HOME']}/%s\"\n".format(validatorPem) +
      "chef_server_url        \"%s\"               \n".format(chefService.endpoint)

  def chefRunShContent =
    "#!/bin/bash            \n" +
    "PATH=$PATH:/sbin:/usr/sbin\n" +
      "chef-client -c %s -j %s\n".format(chefConfig, chefAttrs)

  def chefClientAttrs: JObject

  def cleanUp(signal: Signal) {
    sshClient.disconnect()
  }
}

trait InitialChefRun extends ChefRunPreparer[PrepareInitialChefRun] {
  def chefClientAttrs = "genesis" -> (
      ("genesis_id" -> chefService.genesisId) ~
      ("env_name" -> action.env.name) ~
      ("role_name" -> action.server.roleName) ~
      ("vm_id" -> action.server.id)
    )
}