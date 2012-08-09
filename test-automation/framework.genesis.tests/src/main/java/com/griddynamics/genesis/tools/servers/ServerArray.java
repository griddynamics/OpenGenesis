package com.griddynamics.genesis.tools.servers;

import java.util.Arrays;

public class ServerArray {
	int id;
	int projectId;
	
	String name;
	String description;
	Server[] servers;


	
	public ServerArray() {
		super();
		// TODO Auto-generated constructor stub
	}
	

	public ServerArray(int id, int projectId, String name, String description,
			Server[] servers) {
		super();
		this.id = id;
		this.projectId = projectId;
		this.name = name;
		this.description = description;
		this.servers = servers;
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Server[] getServers() {
		return servers;
	}
	public void setServers(Server[] servers) {
		this.servers = servers;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + projectId;
		result = prime * result + Arrays.hashCode(servers);
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
		ServerArray other = (ServerArray) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (projectId != other.projectId)
			return false;
		if (!Arrays.equals(servers, other.servers))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "ServerArray [id=" + id + ", projectId=" + projectId + ", name="
				+ name + ", description=" + description + ", servers="
				+ Arrays.toString(servers) + "]";
	}



	
	
}
