package com.griddynamics.genesis.test.steps.users;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.users.SuccessfulUserCreationResponse;
import com.griddynamics.genesis.tools.users.UserDetails;

public class CreateUserStepsImplementation extends UserSharedSources {

	@When("I send request to create user with userName <userName> email <email> firstName <firstName> lastName <lastName> jobTitle <title> password <password> and groups <groups>")
	@Aliases(values = { "I send request to create user with userName $userName email $email firstName $firstName lastName $lastName jobTitle $title password $password and groups $groups" })
	public void whenISendCreateUserRequest(
			@Named("userName") String userName, @Named("email") String email,
			@Named("firstName") String firstName,
			@Named("lastName") String lastName, @Named("title") String title,
			@Named("password") String password, @Named("groups") String groups) {
		expUser = new UserDetails(userName, email, firstName, lastName, title,
				password, groups != null ? groups.split(", ") : null);
		request.setUrl(USERS_PATH);
		request.post(expUser);
	}

	@Then("I expect that user was created successfully")
	public void thenIExpectSuccessfulUserCreation() {
		SuccessfulUserCreationResponse actResponse = request.getResponseObject(SuccessfulUserCreationResponse.class);
		SuccessfulUserCreationResponse expResponse = new SuccessfulUserCreationResponse(true,
				expUser);
		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
	}

	@When("I send request to create user with userName <userName> email <email> and password <password>")
	@Aliases(values={"I send request to create user with userName $userName email $email and password $password"})
	public void whenISendCreateUserShortRequest(
			@Named("userName") String userName, @Named("email") String email,
			@Named("password") String password) {
		whenISendCreateUserRequest(userName, email, "firstName", "lastName",
				"title", password, null);
	}	
}
