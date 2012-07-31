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
package com.griddynamics.genesis.jclouds

import org.jclouds.gogrid.{GoGridAsyncClient, GoGridClient}
import com.griddynamics.genesis.model.{VirtualMachine, Environment}
import com.google.common.collect.Iterables
import org.jclouds.gogrid.domain.{Ip, IpType, JobState, PowerCommand}
import org.jclouds.gogrid.options.{AddServerOptions, GetIpListOptions, GetJobListOptions}
import com.griddynamics.executors.provision.VmMetadataFuture
import org.jclouds.compute.ComputeServiceContext
import java.util.Properties
import org.springframework.stereotype.Component
import collection.mutable
import com.griddynamics.genesis.util.Logging

@Component
class GoGridVmCreationStrategyProvider extends JCloudsVmCreationStrategyProvider {
    val name = "gogrid"
    val computeProperties = new Properties
    val ipStackWrapper = new IpStackWrapper()
    def createVmCreationStrategy(nodeNamePrefix: String, computeContext: ComputeServiceContext): VmCreationStrategy =
        new GoGridVmCreationStrategy(nodeNamePrefix, computeContext, ipStackWrapper)
}

class IpStackWrapper() extends Logging {
    val ipStack: mutable.Stack[Ip] = new mutable.Stack[Ip]()
    val lock:AnyRef = new Object
    def pop()(block: => Iterable[Ip]) =  {
        lock.synchronized(
            if (ipStack.isEmpty) {
                log.debug("Stack empty. Filling it %s", this)
                ipStack.pushAll(block)
            } else {
                log.debug("There is addresses available")
            })
        try {
            Some(ipStack.pop())
        } catch {
            case _ => None
        }
    }


}

class GoGridVmCreationStrategy(nodeNamePrefix: String, computeContext: ComputeServiceContext, ipWrapper: IpStackWrapper) extends DefaultVmCreationStrategy(nodeNamePrefix, computeContext) {
    val restContext = computeContext.getProviderSpecificContext[GoGridClient, GoGridAsyncClient]
    val client = restContext.getApi
    val jobServices = client.getJobServices

    private def isServerLatestJobCompleted(instanceName : String) = {
        val jobOptions = new GetJobListOptions.Builder().latestJobForObjectByName(instanceName)
        val latestJob = Iterables.getOnlyElement(jobServices.getJobList(jobOptions))
        JobState.SUCCEEDED == latestJob.getCurrentState
    }

    class GoGridVmMetadataFuture(instanceName : String) extends VmMetadataFuture {
        var vmAdded = false

        def getMetadata = {
            (isServerLatestJobCompleted(instanceName), vmAdded) match {
                case (false, _) => {
                    None
                }
                case (true, false) => {
                    client.getServerServices.power(instanceName, PowerCommand.START)
                    vmAdded = true
                    None
                }
                case (true, true) => {
                    val server = Iterables.getOnlyElement(client.getServerServices.getServersByName(instanceName))
                    Some(server.getId.toString)
                }
            }
        }
    }

    override def createVm(env: Environment, vm: VirtualMachine) = {
      import DefaultVmCreationStrategy._
      val serverOptions = AddServerOptions.Builder
          .withDescription("%s: %s, id = %d".format(VM_GROUP_PREFIX, vm.roleName, vm.id))
      ipWrapper.pop(){
          fillIpStack
      }.map({ ip =>
          log.debug("Got ip %s", ip.getIp)
          val server = client.getServerServices.addServer(group(env, vm), vm.imageId.get, vm.hardwareId.get, ip.getIp, serverOptions)
          new GoGridVmMetadataFuture(server.getName)
      }).getOrElse(throw new IllegalStateException("Cannot find a free ip address for new vm"))
    }

    override protected def group(env: Environment, vm: VirtualMachine) = {
        "%s.%s.%s".format(nodeNamePrefix, vm.id, env.templateName.take(DefaultVmCreationStrategy.APP_NAME_MAXLEN))
          .take(DefaultVmCreationStrategy.VM_GROUP_MAXLEN)
    }

    private def fillIpStack = {
        log.debug("Filling ip stack. I am %s", this)
        val availableIps = findAvailableIps
        val unavailableIps = findUnavailableIps
        import collection.JavaConversions._
        availableIps.filter(p => unavailableIps.find(a => a.getIp == p.getIp).isEmpty).toIterable
    }

    private def findUnavailableIps = {
        val assignedIps = new GetIpListOptions().onlyAssigned().onlyWithType(IpType.PUBLIC)
        client.getIpServices.getIpList(assignedIps)
    }

    private def findAvailableIps = {
        val  unassignedIps = new GetIpListOptions()
          .onlyUnassigned
          .onlyWithType(IpType.PUBLIC)
        client.getIpServices.getIpList(unassignedIps)
    }


}