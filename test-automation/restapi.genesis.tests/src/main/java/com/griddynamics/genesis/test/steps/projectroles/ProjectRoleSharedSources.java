package com.griddynamics.genesis.test.steps.projectroles;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.projectroles.ProjectRoleDetails;

/**
 * Class contains variables shared between all test methods working with Project
 * Role entities.
 * 
 * @author ybaturina
 * 
 */
public class ProjectRoleSharedSources extends GenesisUtilities {
	protected static String[] actListProjectRoles;
	protected static ProjectRoleDetails expProjectRole;
	protected static ProjectRoleDetails actProjectRole;

	protected static final String PROJECT_ROLES_PATH = endpointProperties.getProperty(
			"projectroles.path", "");
	protected static final String PROJECT_ROLE_DETAILS_PATH = endpointProperties.getProperty(
			"projectroledetails.path", "");
	protected static final String PROJECT_USER_PERMISSIONS = endpointProperties.getProperty(
			"userprojectpermissions.path", "");
}
