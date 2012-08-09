package com.griddynamics.genesis.test.steps.users;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.users.SuccessfulUserCreationResponse;

public class DeleteUserStepsImplementation extends UserSharedSources {

	@When("I send request to delete user with name <userName>")
	@Aliases(values = { "I send request to delete user with name $userName" })
	public void whenISendDeleteUserRequest(@Named("userName") String name) {
		ListUsersStepsImplementation.whenISendGetUsersRequest();
		if (doesUserExist(name)) {
			UserDetailsStepsImplementation.whenISendViewUserRequest(name);
			request.setUrl(USERS_PATH + "/" + name);
			request.delete();
		} 
	}

	@Then("I expect that user was deleted successfully")
	public void thenIExpectSuccessfulUserDeletion() {
		SuccessfulUserCreationResponse actResponse = request.getResponseObject(SuccessfulUserCreationResponse.class);
		SuccessfulUserCreationResponse expResponse = new SuccessfulUserCreationResponse(
				true, actUser);
		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
		
	}
}
