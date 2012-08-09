package com.griddynamics.genesis.tools.users;

/**
 * Class containing fields as in object returning in POST user account successful response.
 * 
 * @author ybaturina
 *
 */
public class SuccessfulUserCreationResponse {
	
	public boolean isSuccess;
	public UserDetails result;
	
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
		SuccessfulUserCreationResponse other = (SuccessfulUserCreationResponse) obj;
		if (isSuccess != other.isSuccess)
			return false;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		return true;
	}

	public SuccessfulUserCreationResponse (boolean isSuccess, UserDetails user){
		this.isSuccess=isSuccess;
		this.result=new UserDetails(user);
	}
	
}
