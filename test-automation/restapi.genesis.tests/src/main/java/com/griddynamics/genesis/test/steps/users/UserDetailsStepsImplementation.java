package com.griddynamics.genesis.test.steps.users;

import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.When;

import com.griddynamics.genesis.tools.users.UserDetails;

public class UserDetailsStepsImplementation extends UserSharedSources {

	@When("I send request to view user with name <name>")
	public static void whenISendViewUserRequest(@Named("name") String name) {
		request.setUrl(USERS_PATH + "/" + name);
		request.get();
		actUser = request.getResponseObject(UserDetails.class);
	}

}
