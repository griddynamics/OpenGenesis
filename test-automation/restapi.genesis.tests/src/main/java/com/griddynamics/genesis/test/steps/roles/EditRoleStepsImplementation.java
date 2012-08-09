package com.griddynamics.genesis.test.steps.roles;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonResultObjectResponse;
import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.roles.RoleDetails;

public class EditRoleStepsImplementation extends RoleSharedSources {

	@When("I send request to edit role with name <roleName> and specify users <users> and groups <groups>")
	@Aliases(values = { "I send request to edit role with name $roleName and specify users $users and groups $groups" })
	public void whenISendEditFullRoleRequest(
			@Named("roleName") String name, @Named("users") String users,
			@Named("groups") String groups) {
		expRole = new RoleDetails(name, new String[0], new String[0]);
		expRole.users = CommonTools.processStringValue(users);
		expRole.groups = CommonTools.processStringValue(groups);
		request.setUrl(ROLES_PATH + "/" + name);
		request.put(expRole);
	}

	@Then("I expect that role was changed successfully")
	public void thenIExpectSuccessfulRoleChanges() {
		CommonResultObjectResponse actResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expResponse = CommonResultObjectResponse
				.getResultFromString(true, false, "", "", "", "");
		Assert.assertEquals(request.checkStatusCode200(), true);
		Assert.assertEquals(actResponse, expResponse);
	}

	@When("I send request to edit role with name <roleName> and specify users <users>")
	@Aliases(values = { "I send request to edit role with name $roleName and specify users $users" })
	public void whenISendEditUsersInRoleRequest(
			@Named("roleName") String name, @Named("users") String users) {
		RoleDetailsStepsImplementation.whenISendViewRoleRequest(name);
		actRole.users = CommonTools.processStringValue(users);
		request.setUrl(ROLES_PATH + "/" + name);
		request.put(actRole);
	}

	@When("I send request to edit role with name <roleName> and specify groups <groups>")
	@Aliases(values = { "I send request to edit role with name $roleName and specify groups $groups" })
	public void whenISendEditGroupsInRoleRequest(
			@Named("roleName") String name, @Named("groups") String groups) {
		RoleDetailsStepsImplementation.whenISendViewRoleRequest(name);
		actRole.groups = CommonTools.processStringValue(groups);
		request.setUrl(ROLES_PATH + "/" + name);
		request.put(actRole);
	}

}
