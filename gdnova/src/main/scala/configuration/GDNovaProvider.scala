package com.griddynamics.genesis.configuration;

import com.griddynamics.genesis.model.Environment;
import com.griddynamics.genesis.model.VirtualMachine;

import java.util.Properties
import org.springframework.beans.factory.annotation.Value
import org.jclouds.compute.options.TemplateOptions
import org.gdjclouds.provider.gdnova.v100.GDNovaTemplateOptions
import collection.JavaConversions._
import org.springframework.stereotype.Component
import com.griddynamics.genesis.jclouds.{JCloudsVmCreationStrategyProvider, DefaultVmCreationStrategy}

@Component
class GDNovaProvider  extends JCloudsVmCreationStrategyProvider {

  val name = "gdnova"

  val computeProperties = {
    val properties = new Properties
    properties("gdnova.contextbuilder") = "org.gdjclouds.provider.gdnova.GDNovaContextBuilder"
    properties("gdnova.propertiesbuilder") = "org.jclouds.openstack.nova.NovaPropertiesBuilder"
    properties
  }

  def createVmCreationStrategy(nodeNamePrefix: String, computeContext: org.jclouds.compute.ComputeServiceContext) =
    new DefaultVmCreationStrategy(nodeNamePrefix, computeContext) {

      override protected def templateOptions(env: Environment, vm: VirtualMachine): TemplateOptions = {
        super.templateOptions(env, vm).asInstanceOf[GDNovaTemplateOptions]
          .keyPair(vm.keyPair.getOrElse(throw new IllegalArgumentException("VM keypair property should be specified")))
      }

    }
}