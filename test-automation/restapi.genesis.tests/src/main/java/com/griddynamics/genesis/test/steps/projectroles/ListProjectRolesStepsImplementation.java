package com.griddynamics.genesis.test.steps.projectroles;


import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class ListProjectRolesStepsImplementation extends ProjectRoleSharedSources {

	@When("I send get project roles request")
	public void whenISendGetProjectRolesRequest() {
		request.setUrl(PROJECT_ROLES_PATH); 
		request.get();
		actListProjectRoles = request.getResponseObject(String[].class);
	}

	@Then("I expect to see project roles <roles>")
	public void thenIExpectToSeeProjectRoles(@Named("roles") String[] roles) {
		Assert.assertEquals(request.checkStatusCode200(), true);
		Assert.assertEquals(roles, actListProjectRoles);
	}

}
