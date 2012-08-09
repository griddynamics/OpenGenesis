package com.griddynamics.genesis.tools.projectroles;

import java.util.Arrays;

/**
 * Class containing fields as in Genesis Project Role.
 * 
 * @author ybaturina
 *
 */
public class ProjectRoleDetails {

	public String[] users;
	public String[] groups;
	
	public ProjectRoleDetails(String[] users, String[] groups) {
		this.users=users;
		this.groups=groups;
	}
	
	public ProjectRoleDetails(ProjectRoleDetails details) {
		this.users=details.users;
		this.groups=details.groups;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(groups);
		result = prime * result + Arrays.hashCode(users);
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
		ProjectRoleDetails other = (ProjectRoleDetails) obj;
		if (!Arrays.equals(groups, other.groups))
			return false;
		if (!Arrays.equals(users, other.users))
			return false;
		return true;
	}

	


}
