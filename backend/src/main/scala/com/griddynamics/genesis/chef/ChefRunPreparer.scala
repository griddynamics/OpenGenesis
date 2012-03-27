/**
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
package com.griddynamics.genesis.chef

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

abstract class ChefRunPreparer[A <: PrepareChefRun](val action: A,
                                                    sshService: SshService,
                                                    val chefService: ChefService)
    extends SyncActionExecutor with Logging {

    val runDir = genesisDir / action.label

    val chefLog = runDir / "chef.log"
    val chefAttrs = runDir / "attrs.json"
    val chefConfig = runDir / "client.rb"

    val execDetails = new ExecDetails(action.env, action.vm, runDir / "chef-run.sh", runDir)

    val chefLogLevel = ":info"

    lazy val sshClient = sshService.sshClient(action.env, action.vm)

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
        "node_name              \"%s\"               \n".format(action.vm(ChefVmAttrs.ChefNodeName)) +
        "client_key             \"#{ENV['HOME']}/%s\"\n".format(clientPem) +
        "validation_client_name \"%s\"               \n".format(chefService.validatorCredentials.identity) +
        "validation_key         \"#{ENV['HOME']}/%s\"\n".format(validatorPem) +
        "chef_server_url        \"%s\"               \n".format(chefService.endpoint)

    def chefRunShContent =
        "#!/bin/bash            \n" +
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
        ("role_name" -> action.vm.roleName) ~
        ("vm_id" -> action.vm.id)
    )
}

trait RegularChefRun extends ChefRunPreparer[PrepareRegularChefRun] {
    def chefClientAttrs = {
        val runList = JArray(action.runList.map(JString(_)).toList)
        new JObject(JField("run_list", runList) +: action.jattrs.obj.filter(_.name != "run_list"))
    }
}
