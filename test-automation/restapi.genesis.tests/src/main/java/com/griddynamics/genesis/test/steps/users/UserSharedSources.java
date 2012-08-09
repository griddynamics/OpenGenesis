package com.griddynamics.genesis.test.steps.users;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.users.UserDetails;

/**
 * Class contains variables shared between all test methods working with User
 * Account entities.
 * 
 * @author ybaturina
 * 
 */
public class UserSharedSources extends GenesisUtilities {
	protected static UserDetails[] actListUsers;
	protected static UserDetails expUser;
	protected static UserDetails actUser;

	protected static final String USERS_PATH = endpointProperties.getProperty(
			"users.path", "");

	protected static boolean doesUserExist(String userName) {
		for (UserDetails actUs : actListUsers) {
			if (actUs.username.equals(userName)) {
				return true;
			}
		}
		return false;
	}
}
