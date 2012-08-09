package com.griddynamics.genesis.test.steps.usergroups;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.usergroups.UserGroupDetails;

/**
 * Class contains variables shared between all test methods working with User
 * Group entities.
 * 
 * @author ybaturina
 * 
 */
public class UserGroupSharedSources extends GenesisUtilities {
	protected static UserGroupDetails[] actListGroups;
	protected static UserGroupDetails expGroup;
	protected static UserGroupDetails actGroup;

	protected static final String GROUPS_PATH = endpointProperties.getProperty(
			"groups.path", "");

	protected static int getUserGroupId(String groupName) {
		for (UserGroupDetails actGr : actListGroups) {
			if (actGr.name.equals(groupName)) {
				return actGr.id;
			}
		}
		return 0;
	}
}
