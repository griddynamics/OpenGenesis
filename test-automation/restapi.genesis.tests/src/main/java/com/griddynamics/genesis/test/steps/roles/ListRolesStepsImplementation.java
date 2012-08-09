package com.griddynamics.genesis.test.steps.roles;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class ListRolesStepsImplementation extends RoleSharedSources {

	@When("I send get roles request")
	public void whenISendGetRolesRequest() {
		request.setUrl(ROLES_PATH);
		request.get();
		actListRoles = request.getResponseObject(String[].class);
	}

	@Then("I expect to see roles <roles>")
	public void thenIExpectToSeeRoles(@Named("roles") String[] roles) {
		Assert.assertEquals(roles, actListRoles);
	}

}
