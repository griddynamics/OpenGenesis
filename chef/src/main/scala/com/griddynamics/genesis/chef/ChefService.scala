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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.chef

import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString
import com.griddynamics.genesis.service.Credentials
import com.griddynamics.genesis.chef.rest.{Role, DatabagItem, ChefRestClient}
import com.griddynamics.genesis.model.{EnvResource, VirtualMachine, Environment}

trait ChefService {
    def genesisId : String

    def endpoint : String

    def validatorCredentials : Credentials

    def chefClientName(env : Environment, server : EnvResource) : String

    def createDatabag(env : Environment, name : String, items : Map[String, JObject], overwrite : Boolean)

    def createRole(env : Environment, name : String, description : String, runList : Seq[String],
                   defaults : JObject, overrides : JObject, overwrite : Boolean)

    def deleteChefEnv(env : Environment)
}

class ChefServiceImpl(val genesisId: String, val endpoint : String,
                      val validatorCredentials : Credentials, chefClient : ChefRestClient) extends ChefService {
    def chefObjectName(env : Environment, objectName : String) = {
        "genesis_%s_%s_%s".format(genesisId, env.name, objectName)
    }

    def isObjectInEnv(objectName : String, env : Environment) = {
        objectName.startsWith("genesis_%s_%s_".format(genesisId, env.name))
    }

    def chefClientName(env : Environment, vm : EnvResource) = {
        chefObjectName(env, "%s_%d".format(vm.roleName, vm.id))
    }

    def createDatabag(env : Environment, name : String, items : Map[String, JObject], overwrite : Boolean) {
        val databagName = chefObjectName(env, name)

        if (overwrite){
            chefClient.listDatabags().find(_ == databagName).foreach(chefClient.deleteDatabag(_))
        }

        chefClient.createDatabag(databagName)

        for ((itemName, attrs) <- items) {
            val idAttrs = JField("id", JString(itemName)) +: attrs.obj.filter(_.name != "id")
            val item = new DatabagItem(itemName, JObject(idAttrs))
            chefClient.createDatabagItem(databagName, item)
        }
    }

    def createRole(env : Environment, name : String, description : String, runList : Seq[String],
                   defaults : JObject, overrides : JObject, overwrite : Boolean) {
        val roleName = chefObjectName(env, name)

        val chefRole = new Role(roleName, description, runList, defaults, overrides)

        if (overwrite) {
            chefClient.listRoles().find(_ == roleName).foreach(chefClient.deleteRole(_))
        }

        chefClient.createRole(chefRole)
    }

    def deleteChefEnv(env : Environment) {
        deleteClients(env)
        deleteNodes(env)
        deleteRoles(env)
        deleteDatabags(env)
    }

    def deleteClients(env : Environment) {
        for (client <- chefClient.listClients()) {
            if (isObjectInEnv(client, env)) {
                chefClient.deleteClient(client)
            }
        }
    }

    def deleteNodes(env : Environment) {
        for (node <- chefClient.listNodes()) {
            if (isObjectInEnv(node, env)) {
                chefClient.deleteNode(node)
            }
        }
    }

    def deleteDatabags(env : Environment) {
        for (databag <- chefClient.listDatabags()) {
            if (isObjectInEnv(databag, env)) {
                chefClient.deleteDatabag(databag)
            }
        }
    }

    def deleteRoles(env : Environment) {
       for (role <- chefClient.listRoles()) {
            if (isObjectInEnv(role, env)) {
                chefClient.deleteRole(role)
            }
        }
    }
}
