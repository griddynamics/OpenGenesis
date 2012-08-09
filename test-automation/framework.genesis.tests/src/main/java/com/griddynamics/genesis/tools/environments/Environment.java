package com.griddynamics.genesis.tools.environments;


import java.util.List;

public class Environment {
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

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Environment other = (Environment) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (templateName == null) {
			if (other.templateName != null)
				return false;
		} else if (!templateName.equals(other.templateName))
			return false;
		if (templateVersion == null) {
			if (other.templateVersion != null)
				return false;
		} else if (!templateVersion.equals(other.templateVersion))
			return false;
		if (vms == null) {
			if (other.vms != null)
				return false;
		} else if (!vms.equals(other.vms))
			return false;
		return true;
	}

	
}
