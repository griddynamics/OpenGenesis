package com.griddynamics.genesis.plugin.adapter;

import com.griddynamics.genesis.plugin.PartialStepCoordinatorFactory;
import com.griddynamics.genesis.plugin.PluginConfigurationContext;
import scala.collection.JavaConversions;

import java.util.Map;

abstract public class AbstractPartialStepCoordinatorFactory implements PartialStepCoordinatorFactory {

  private String pluginId;
  private PluginConfigurationContext pluginConfiguration;

  public AbstractPartialStepCoordinatorFactory(String pluginId, PluginConfigurationContext pluginConfiguration) {
    this.pluginId = pluginId;
    this.pluginConfiguration = pluginConfiguration;
  }

  public Map<String, String> getConfig() {
    return JavaConversions.mapAsJavaMap(pluginConfiguration.configuration(pluginId));
  }

}
