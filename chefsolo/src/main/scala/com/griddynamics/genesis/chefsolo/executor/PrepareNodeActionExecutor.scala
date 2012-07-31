/*
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
package com.griddynamics.genesis.chefsolo.executor

import com.griddynamics.genesis.workflow.SimpleSyncActionExecutor
import org.jclouds.io.payloads.FilePayload
import com.griddynamics.genesis.chefsolo.action.{NodePrepared, PrepareNodeAction}
import java.io.File
import org.jclouds.ssh.SshClient
import com.griddynamics.genesis.util.shell.Path

import com.griddynamics.genesis.util.Logging
import com.griddynamics.genesis.service.{Credentials, SshService}
import com.griddynamics.genesis.exec.{ExecNodeInitializer, ExecDetails}
import com.griddynamics.genesis.util.shell.command.{mkdir, chmod}

class PrepareNodeActionExecutor(override val action: PrepareNodeAction,
                                val sshService: SshService) extends SimpleSyncActionExecutor with Logging  {

    lazy val sshClient: SshClient = sshService.sshClient(action.env, action.server)

    val runDir : Path = ExecNodeInitializer.genesisDir / action.label
    val jsonResource = runDir / "local.json"
    val remoteCookbooks = runDir / "cookbooks.tar.gz"
    val shPath = runDir / "run_chef.sh"
    val soloRbPath = runDir / "solo.rb"

    def copyChefResources(sshClient: SshClient, homeDir: String) {
        sshClient.exec(mkdir(runDir))
        sshClient.put(jsonResource, action.json)
        sshClient.put(remoteCookbooks, new FilePayload(new File(action.cookbooksPath)))
        sshClient.put(soloRbPath, soloRbContents(homeDir + "/" + runDir))
        sshClient.put(shPath, chefRunShContent(homeDir + "/" + runDir))
        sshClient.exec(chmod("0555", shPath))
        sshClient.exec(tar("zxvf", remoteCookbooks, "-C", runDir))
    }

    def soloRbContents(runDir: String) = {
        "file_cache_path \"/var/chefsolo\"\n" +
        "cookbook_path [\"%s/cookbooks\", \"%s/site-cookbooks\"]\n".format(runDir, runDir) +
        "data_bag_path \"/var/chef-solo/databags\"\n"
    }

    def chefRunShContent(runDir: String) = {
        "#!/bin/bash\n" +
        "echo \"Running chef-solo\"\n" +
        "chef-solo -c %s -j %s | tee %s/chef-deploy.log\n".format(runDir + "/solo.rb", runDir + "/local.json", runDir) +
        "RET=${PIPESTATUS[0]}\n" +
        "echo \"Finished\"\n" +
        "exit $RET\n"
    }

    def startSync() = {
        var homeDir = ""
        PrepareNodeActionExecutor.sshDo(sshClient) {
            c => {
                homeDir = PrepareNodeActionExecutor.homeDir(c)
                copyChefResources(c, homeDir)
            }
        }
        val installDetails = ExecDetails(action.env, action.server, shPath, runDir)
        NodePrepared(action, action.server, installDetails)
    }
}

import com.griddynamics.genesis.util.shell.Command

object install extends Command("install")

object mv extends Command("mv")

object tar extends Command("tar")

object PrepareNodeActionExecutor {
    def sshDo(client: SshClient)(block: (SshClient) => Any) = {
        client.connect()
        try {
            block(client)
        } finally {
            client.disconnect()
        }
    }

    def homeDir(sshClient: SshClient)  = {
        sshClient.exec("cat /etc/passwd | grep `whoami` | sed 's/.*:\\([^:]*\\):[^:]*/\\1/'")
          .getOutput.takeWhile(_ != '\n').trim
    }

}
