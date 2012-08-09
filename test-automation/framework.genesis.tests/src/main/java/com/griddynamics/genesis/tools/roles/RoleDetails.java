package com.griddynamics.genesis.tools.roles;

import java.util.Arrays;

/**
 * Class containing fields as in Genesis System Role.
 * 
 * @author ybaturina
 *
 */
public class RoleDetails {

	public String name;
	public String[] users;
	public String[] groups;
	
	public RoleDetails(String name, String[] users, String[] groups) {
		this.name=name;
		this.users=users;
		this.groups=groups;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(groups);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		RoleDetails other = (RoleDetails) obj;
		if (!Arrays.equals(groups, other.groups))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(users, other.users))
			return false;
		return true;
	}


}
