/*
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

package com.griddynamics.genesis.model

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

class XmlSerializeTest extends AssertionsForJUnit {

  private val DEP_ATTRS = Seq(DeploymentAttribute("applicationVersion", "1.0.0", "Application Version"),
     DeploymentAttribute("attr1", "value", "Deployment Attribute 1"))
  private val IP = IpAddresses(privateIp = Option("private"))
  private val KEY_PAIR = "keyPair1"

  private val XML_DEP_ATTRS =
    """<list>
  <attribute>
    <key>applicationVersion</key>
    <value>1.0.0</value>
    <desc>Application Version</desc>
  </attribute>
  <attribute>
    <key>attr1</key>
    <value>value</value>
    <desc>Deployment Attribute 1</desc>
  </attribute>
</list>"""

  import AttrsSerialization.{toXML, fromXML}

   @Test def testSerializeEnvAttrs() {
     val env = new Environment()
     env.deploymentAttrs = DEP_ATTRS
     expectResult(XML_DEP_ATTRS)(toXML(env(Environment.DeploymentAttr)))
   }

  @Test def testDeserializeEnvAttrs() {
     expectResult(DEP_ATTRS)(fromXML(XML_DEP_ATTRS))
  }

  import VirtualMachine._
  @Test def testVmAttrs() {
    val vm = new VirtualMachine()
    vm(IpAttr) = IP
    val ipXml = toXML(vm(IpAttr))
    expectResult(IP)(fromXML(ipXml))
    vm.keyPair = Option(KEY_PAIR)
    val kpXml = toXML(vm(KeyPair))
    expectResult(KEY_PAIR) (fromXML(kpXml))
  }
}
