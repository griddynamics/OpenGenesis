package com.griddynamics.genesis.tools.environments;

public class GetEnvironmentListResponse {
    String name;
    String status;
    String creator;
    String templateName;
    String templateVersion;

    
 	public GetEnvironmentListResponse() {
 		super();
 	}

 	public GetEnvironmentListResponse(String name, String status,
			String creator, String templateName, String templateVersion) {
		super();
		this.name = name;
		this.status = status;
		this.creator = creator;
		this.templateName = templateName;
		this.templateVersion = templateVersion;
	}
    
 	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public String getTemplateVersion() {
		return templateVersion;
	}
	public void setTemplateVersion(String templateVersion) {
		this.templateVersion = templateVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ ((templateName == null) ? 0 : templateName.hashCode());
		result = prime * result
				+ ((templateVersion == null) ? 0 : templateVersion.hashCode());
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
		GetEnvironmentListResponse other = (GetEnvironmentListResponse) obj;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
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
		return true;
	}
	
	
}

