package com.griddynamics.genesis.tools.credentials;


public class AddCredentialsResponse {
	public boolean isSuccess;
	public Credentials result;
	
	public AddCredentialsResponse() {
		super();
	}
	
	public AddCredentialsResponse(boolean isSuccess, Credentials credentials) {
		super();
		this.isSuccess = isSuccess;
		this.result = credentials;
	}


	public boolean isSuccess() {
		return isSuccess;
	}
	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	public Credentials getCredentials() {
		return result;
	}
	public void setCredentials(Credentials credentials) {
		this.result = credentials;
	}


	@Override
	public String toString() {
		return "AddCredentialsResponse [isSuccess=" + isSuccess
				+ ", credentials=" + result + "]";
	}


	
}
