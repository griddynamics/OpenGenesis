package com.griddynamics.genesis.tools.usergroups;

import java.util.Arrays;

/**
 * Class containing fields as in Genesis System User Group.
 * 
 * @author ybaturina
 *
 */
public class UserGroupDetails {

	public String mailingList;
	public String description;
	public String name;
	public String[] users;
	public int id;

	public UserGroupDetails(int id, String name, String description,
			String mail, String[] users) {
		this.users = users;
		this.name = name;
		this.mailingList = mail;
		this.description = description;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + id;
		result = prime * result
				+ ((mailingList == null) ? 0 : mailingList.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(users);
		return result;
	}

	public boolean equals(Object obj, boolean ignoreUsers) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserGroupDetails other = (UserGroupDetails) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		// if (id != other.id)
		// return false;
		if (mailingList == null) {
			if (other.mailingList != null)
				return false;
		} else if (!mailingList.equals(other.mailingList))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!ignoreUsers) {
			if (!Arrays.equals(users, other.users))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean equals(Object obj) {
		return equals(obj, true);
	}

}
