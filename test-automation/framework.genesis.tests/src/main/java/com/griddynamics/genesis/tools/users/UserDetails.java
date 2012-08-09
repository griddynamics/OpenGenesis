package com.griddynamics.genesis.tools.users;

import java.util.Arrays;

/**
 * Class containing fields as in Genesis System User Account.
 * 
 * @author ybaturina
 *
 */
public class UserDetails {

	public String username=null;
	public String email=null;
	public String firstName=null;
	public String lastName=null;
	public String jobTitle=null;
	public String password=null;
	public String[] groups=null;

	public UserDetails(String userName, String email, String firstName,
			String lastName, String jobTitle, String password, String[] groups) {
		this.username = userName;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.jobTitle = jobTitle;
		this.password = password;
		this.groups = groups;
	}
	
	public UserDetails(){
		
	}
	
	public UserDetails(UserDetails other) {
		this(other.username, other.email, other.firstName, other.lastName,
				other.jobTitle, other.password, other.groups);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result
				+ ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + Arrays.hashCode(groups);
		result = prime * result
				+ ((jobTitle == null) ? 0 : jobTitle.hashCode());
		result = prime * result
				+ ((lastName == null) ? 0 : lastName.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	public boolean equals(Object obj, boolean ignorePassword,
			boolean ignoreGroups) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserDetails other = (UserDetails) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (firstName == null) {
			if (other.firstName != null)
				return false;
		} else if (!firstName.equals(other.firstName))
			return false;
		if (!ignoreGroups) {
			if (!Arrays.equals(groups, other.groups))
				return false;
		}
		if (jobTitle == null) {
			if (other.jobTitle != null)
				return false;
		} else if (!jobTitle.equals(other.jobTitle))
			return false;
		if (lastName == null) {
			if (other.lastName != null)
				return false;
		} else if (!lastName.equals(other.lastName))
			return false;
		if (!ignorePassword) {
			if (password == null) {
				if (other.password != null)
					return false;
			} else if (!password.equals(other.password))
				return false;
		}
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UserDetails [username=" + username + ", email=" + email
				+ ", firstName=" + firstName + ", lastName=" + lastName
				+ ", jobTitle=" + jobTitle + ", password=" + password
				+ ", groups=" + (groups==null?"null":Arrays.toString(groups)) + "]";
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, true, true);
	}

}
