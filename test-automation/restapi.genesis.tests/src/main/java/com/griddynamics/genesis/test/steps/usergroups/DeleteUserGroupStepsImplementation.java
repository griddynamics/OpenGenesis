package com.griddynamics.genesis.test.steps.usergroups;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

public class DeleteUserGroupStepsImplementation extends UserGroupSharedSources {

	@When("I send request to delete user group with name <groupName>")
	@Aliases (values={"I send request to delete user group with name $groupName"})
	public void whenISendDeleteUserGroupRequest(@Named("groupName") String name) {
			ListUserGroupsStepsImplementation.whenISendGetUserGroupsRequest();
			int id=getUserGroupId(name);
			request.setUrl(GROUPS_PATH+"/" + id);
			request.delete();
	}

	@Then("I expect that user group was deleted successfully")
	public void thenIExpectSuccessfulGroupDeletion() {
		Assert.assertEquals(request.checkStatusCode200(), true);
	}
}
