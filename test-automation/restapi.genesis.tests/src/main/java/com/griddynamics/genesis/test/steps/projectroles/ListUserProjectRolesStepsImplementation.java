package com.griddynamics.genesis.test.steps.projectroles;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;

public class ListUserProjectRolesStepsImplementation extends ProjectRoleSharedSources {

	@When("I send get user permissions in project <projectName> request")
		@Aliases(values = { "I send get user permissions in project $projectName request" })
	public void whenISendGetUserPermissionsInProjectRequest(@Named("projectName") String projectName) {
		int id=getProjectIdByProjectName(projectName);

		request.setUrl(String.format(PROJECT_USER_PERMISSIONS,id)); 
		request.get();
	}

	@Then("I expect to see user permissions <permissions>")
	public void thenIExpectToSeeUserPermissions(@Named("permissions") String permissions) {
		actListProjectRoles = request.getResponseObject(String[].class);
		Assert.assertEquals(actListProjectRoles, CommonTools.processStringValue(permissions));
	}

}
