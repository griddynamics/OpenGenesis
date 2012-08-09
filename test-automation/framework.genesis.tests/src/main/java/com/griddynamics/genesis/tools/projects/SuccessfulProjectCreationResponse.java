package com.griddynamics.genesis.tools.projects;

/**
 * Class containing fields as in object returning in POST project role successful response.
 * 
 * @author ybaturina
 *
 */
public class SuccessfulProjectCreationResponse {
	
	public boolean isSuccess;
	public ProjectDetails result;
	
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
		SuccessfulProjectCreationResponse other = (SuccessfulProjectCreationResponse) obj;
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
		return "SuccessfulProjectCreationResponse [isSuccess=" + isSuccess
				+ ", result=" + result + "]";
	}

	public SuccessfulProjectCreationResponse (boolean isSuccess, ProjectDetails proj){
		this.isSuccess=isSuccess;
		this.result=new ProjectDetails(proj.id, proj.name, proj.description, proj.projectManager);
	}
	
}
