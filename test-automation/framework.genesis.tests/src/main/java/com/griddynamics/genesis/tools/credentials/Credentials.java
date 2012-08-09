package com.griddynamics.genesis.tools.credentials;

public class Credentials {

	int id;
	int projectId;

	String pairName;
	String cloudProvider;
	String identity;
	String credential;

	String fingerPrint;
	
	public Credentials() {
		super();
		// TODO Auto-generated constructor stub
	}
	
		
	public Credentials(String pairName, String cloudProvider, String identity,
			String credential) {
		super();
		this.pairName = pairName;
		this.cloudProvider = cloudProvider;
		this.identity = identity;
		this.credential = credential;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getProjectId() {
		return projectId;
	}


	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}


	public String getFingerPrint() {
		return fingerPrint;
	}


	public void setFingerPrint(String fingerPrint) {
		this.fingerPrint = fingerPrint;
	}


	public String getPairName() {
		return pairName;
	}
	public void setPairName(String pairName) {
		this.pairName = pairName;
	}
	public String getCloudProvider() {
		return cloudProvider;
	}
	public void setCloudProvider(String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public String getCredential() {
		return credential;
	}
	public void setCredential(String credential) {
		this.credential = credential;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cloudProvider == null) ? 0 : cloudProvider.hashCode());
		result = prime * result
				+ ((credential == null) ? 0 : credential.hashCode());
		result = prime * result
				+ ((identity == null) ? 0 : identity.hashCode());
		result = prime * result
				+ ((pairName == null) ? 0 : pairName.hashCode());
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
		Credentials other = (Credentials) obj;
		if (cloudProvider == null) {
			if (other.cloudProvider != null)
				return false;
		} else if (!cloudProvider.equals(other.cloudProvider))
			return false;
		if (credential == null) {
			if (other.credential != null)
				return false;
		} else if (!credential.equals(other.credential))
			return false;
		if (identity == null) {
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		if (pairName == null) {
			if (other.pairName != null)
				return false;
		} else if (!pairName.equals(other.pairName))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Credentials [id=" + id + ", projectId=" + projectId
				+ ", pairName=" + pairName + ", cloudProvider=" + cloudProvider
				+ ", identity=" + identity + ", credential=" + credential
				+ ", fingerPrint=" + fingerPrint + "]";
	}
		
	
	
}
