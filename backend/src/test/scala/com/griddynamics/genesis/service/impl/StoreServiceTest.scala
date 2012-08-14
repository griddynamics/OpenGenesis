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
package com.griddynamics.genesis.service.impl

import org.squeryl.adapters.H2Adapter
import org.squeryl.{Session, SessionFactory}
import java.sql.DriverManager
import org.junit.{Before, Test, BeforeClass}

import org.squeryl.PrimitiveTypeMode.transaction
import org.scalatest.junit.MustMatchersForJUnit
import com.griddynamics.genesis.model._

class StoreServiceTest extends MustMatchersForJUnit {
    val storeService = new StoreService

    var project: Project = _
    var env: Environment = _
    var workflow: Workflow = _

    var vm1: VirtualMachine = _
    var vm2: VirtualMachine = _

    import  StoreServiceTest._
    import VirtualMachine.IpAttr

    @Test def testAttrs() {
        val vms = transaction {
            storeService.listVms(env)
        }

        val vm11 = vms.filter(_.id == vm1.id).head
        val vm22 = vms.filter(_.id == vm2.id).head
        vm11(IpAttr) must be === IP1
        vm22(IpAttr) must be === IP2

    }

    @Test def testRetrieveWorkflow() {
        transaction {
            storeService.retrieveWorkflow(env.id, env.projectId)
        }
    }

    @Test def testFindVmById() {
        val (_, vm) = transaction {
            storeService.getVm("i1")
        }

        vm must be === vm1
    }

    @Test def testFindEnv {
        val e = transaction { storeService.findEnv(env.name, env.projectId)}
        e must be (Some(env))
        e.get(EnvAttr1) must be (EnvAttrVal1)
        e.get(EnvAttr2) must be (EnvAttrVal2)
    }

    @Before def before() {
        val jdbcUrl = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1".format(java.lang.System.currentTimeMillis)

        SessionFactory.concreteFactory = Some(() =>
            new Session(DriverManager.getConnection(jdbcUrl), adapter)
        )

        transaction {
            GenesisSchema.create
            fillDb()
        }
    }

    def fillDb() {

        project =  GenesisSchema.projects.insert(new Project("project1", Some("test project"), "Nina, Soto"));

        env = new Environment("env", EnvStatus.Busy, "owner", "template", "0.1", project.id)
        env(EnvAttr1) = EnvAttrVal1
        workflow = new Workflow(env.id, "workflow", "owner", WorkflowStatus.Requested, 0, 0, Map[String, String](), None, None)

        val (e, w) = storeService.createEnv(env, workflow).right.get
        env = e
        workflow = w

        vm1 = new VirtualMachine(env.id, 0, 0, VmStatus.Provision, "vm1",  Some("i1"), Option("1"), Option("95"))
        vm1(IpAttr) = IP1

        vm2 = new VirtualMachine(env.id, 0, 0, VmStatus.Provision, "vm2",  Some("i2"), Option("1"), Option("95"))
        vm2(IpAttr) = IP2

        vm1 = storeService.createVm(vm1)
        vm2 = storeService.createVm(vm2)

        env(EnvAttr2) = EnvAttrVal2
        storeService.updateEnv(env)
    }
}

object StoreServiceTest {
    val adapter = new H2Adapter
    val IP1 = IpAddresses(publicIp=Some("172.18.128.54"))
    val IP2 = IpAddresses(privateIp=Some("127.0.0.1"))
    
    val EnvAttr1 = EntityAttr[String]("project")
    val EnvAttrVal1 = "Test Project"

    val EnvAttr2 = EntityAttr[String]("tag")
    val EnvAttrVal2 = "Test Tag"

    @BeforeClass def beforeClass() {
        Class.forName("org.h2.Driver")
    }
}
