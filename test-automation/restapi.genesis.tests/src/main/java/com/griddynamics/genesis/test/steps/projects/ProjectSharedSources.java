package com.griddynamics.genesis.test.steps.projects;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.griddynamics.genesis.tools.projects.ProjectDetails;

/**
 * Class contains variables shared between all test methods working with Project
 * entities.
 * 
 * @author ybaturina
 * 
 */
public class ProjectSharedSources extends GenesisUtilities {
	protected static ProjectDetails[] actListProjects;
	protected static ProjectDetails expProject;

	protected static final String PROJECTS_PATH = endpointProperties
			.getProperty("projects.path", "");

}
