package com.griddynamics.genesis.tools.servers;


public class CreateServerArrayResponse {

	public boolean isSuccess;
	public ServerArray result;

	public CreateServerArrayResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CreateServerArrayResponse(boolean isSuccess, ServerArray result) {
		super();
		this.isSuccess = isSuccess;
		this.result = result;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public ServerArray getArray() {
		return result;
	}

	public void setArray(ServerArray result) {
		this.result = result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isSuccess ? 1231 : 1237);
		result = prime * result
				+ ((this.result == null) ? 0 : this.result.hashCode());
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
		CreateServerArrayResponse other = (CreateServerArrayResponse) obj;
		if (isSuccess != other.isSuccess)
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CreateServerArrayResponse [isSuccess=" + isSuccess
				+ ", result=" + result + "]";
	}
	
	
	

}
