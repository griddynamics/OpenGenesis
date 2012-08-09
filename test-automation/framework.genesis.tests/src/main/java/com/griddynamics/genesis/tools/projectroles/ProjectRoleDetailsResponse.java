package com.griddynamics.genesis.tools.projectroles;

/**
 * Class containing fields as in object returning in POST project role successful response.
 * 
 * @author ybaturina
 *
 */
public class ProjectRoleDetailsResponse {

	public ProjectRoleDetails result;
	public String isSuccess;
	
	public ProjectRoleDetailsResponse(ProjectRoleDetails details, String isSuccess) {
		this.result=new ProjectRoleDetails(details);
		this.isSuccess=isSuccess;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((isSuccess == null) ? 0 : isSuccess.hashCode());
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
		ProjectRoleDetailsResponse other = (ProjectRoleDetailsResponse) obj;
		if (isSuccess == null) {
			if (other.isSuccess != null)
				return false;
		} else if (!isSuccess.equals(other.isSuccess))
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		return true;
	}

	
}
