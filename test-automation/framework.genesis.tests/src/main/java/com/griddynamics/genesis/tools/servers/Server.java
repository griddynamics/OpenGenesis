package com.griddynamics.genesis.tools.servers;

public class Server {

	int id;
	int arrayId;
	String instanceId;
	String address;

	
	public Server() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Server(int id, int arrayId, String instanceId, String address) {
		super();
		this.id = id;
		this.arrayId = arrayId;
		this.instanceId = instanceId;
		this.address = address;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getArrayId() {
		return arrayId;
	}


	public void setArrayId(int arrayId) {
		this.arrayId = arrayId;
	}


	public String getInstanceId() {
		return instanceId;
	}


	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}


	public String getAddress() {
		return address;
	}


	public void setAddress(String address) {
		this.address = address;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + arrayId;
		result = prime * result + id;
		result = prime * result
				+ ((instanceId == null) ? 0 : instanceId.hashCode());
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
		Server other = (Server) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (arrayId != other.arrayId)
			return false;
		if (id != other.id)
			return false;
		if (instanceId == null) {
			if (other.instanceId != null)
				return false;
		} else if (!instanceId.equals(other.instanceId))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Server [id=" + id + ", arrayId=" + arrayId + ", instanceId="
				+ instanceId + ", address=" + address + "]";
	}
	
	
	
}
