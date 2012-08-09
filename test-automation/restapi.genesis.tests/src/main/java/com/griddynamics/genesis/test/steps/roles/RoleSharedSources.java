package com.griddynamics.genesis.test.steps.roles;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.roles.RoleDetails;

/**
 * Class contains variables shared between all test methods working with
 * Role entities.
 * 
 * @author ybaturina
 * 
 */
public class RoleSharedSources extends GenesisUtilities {
	protected static String[] actListRoles;
	protected static RoleDetails expRole;
	protected static RoleDetails actRole;

	protected static final String ROLES_PATH = endpointProperties.getProperty(
			"roles.path", "");
}
