package com.griddynamics.genesis.tools.environments;

import java.util.Map;

public class CreateEnvironmentRequest {
    String envName;
    String templateVersion;
    String templateName;

    Map variables;

    public CreateEnvironmentRequest() {
    }

    public CreateEnvironmentRequest(String envName, String templateName, String templateVersion, Map variables) {
        this.templateVersion = templateVersion;
        this.templateName = templateName;
        this.variables = variables;
        this.envName = envName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map getVariables() {
        return variables;
    }

    public void setVariables(Map variables) {
        this.variables = variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreateEnvironmentRequest that = (CreateEnvironmentRequest) o;

        if (!envName.equals(that.envName)) return false;
        if (!templateName.equals(that.templateName)) return false;
        if (!templateVersion.equals(that.templateVersion)) return false;
        if (!variables.equals(that.variables)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = templateVersion.hashCode();
        result = 31 * result + templateName.hashCode();
        result = 31 * result + variables.hashCode();
        result = 31 * result + envName.hashCode();
        return result;
    }
}
