package com.griddynamics.genesis.tools.projects;

/**
 * Class containing fields as in Genesis Project.
 * 
 * @author ybaturina
 *
 */
public class ProjectDetails {

	public int id;
	public String description;
	public String name;
	public String projectManager;
	
	
	public ProjectDetails() {
		super();
	}

	public ProjectDetails(int id, String name, String description, String manager) {
		this.id=id;
		this.name=name;
		this.projectManager=manager;
		this.description=description;
	}

	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProjectManager() {
		return projectManager;
	}

	public void setProjectManager(String projectManager) {
		this.projectManager = projectManager;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result + ((projectManager == null) ? 0 : projectManager.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	
	
	@Override
	public String toString() {
		return "ProjectDetails [id=" + id + ", description=" + description
				+ ", name=" + name + ", projectManager=" + projectManager + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProjectDetails other = (ProjectDetails) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		//if (id != other.id)
			//return false;
		if (projectManager == null) {
			if (other.projectManager != null)
				return false;
		} else if (!projectManager.equals(other.projectManager))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
