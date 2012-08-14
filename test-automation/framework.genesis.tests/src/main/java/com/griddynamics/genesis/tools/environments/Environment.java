package com.griddynamics.genesis.tools.environments;


import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.List;

public class Environment {
    Integer id;
    String name;
    String projectId;
    String status;
    String ip;
    String templateVersion;
    String templateName;
    List<Vms> vms;
    
    
	public Environment() {
		super();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getProjectId() {
		return projectId;
	}
	
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
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
	
	public List<Vms> getVms() {
		return vms;
	}

	public void setVms(List<Vms> vms) {
		this.vms = vms;
	}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + ((id == null) ? 0 : id);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ ((templateName == null) ? 0 : templateName.hashCode());
		result = prime * result
				+ ((templateVersion == null) ? 0 : templateVersion.hashCode());
		result = prime * result + ((vms == null) ? 0 : vms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
	}


}
