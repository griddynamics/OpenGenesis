package com.griddynamics.genesis.test.steps.users;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.users.UserDetails;

public class ListUsersStepsImplementation extends UserSharedSources {

	@When("I send get users request")
	public static void whenISendGetUsersRequest() {
		request.setUrl(USERS_PATH);
		request.get();
		actListUsers = request.getResponseObject(UserDetails[].class);
	}

	@Then("I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result <result>")
	@Aliases(values = { "I expect to see user with userName <userName> email <email> firstName <firstName> lastName <lastName> and jobTitle <title> with result $result" })
	public void thenIExpectToSeeUser(@Named("userName") String userName,
			@Named("email") String email, @Named("firstName") String firstName,
			@Named("lastName") String lastName,
			@Named("title") String title, @Named("result") Boolean result) {
		boolean res = false;
		UserDetails expUser = new UserDetails(userName, email, firstName,
				lastName, title, "", new String[0]);
		whenISendGetUsersRequest();
		for (UserDetails actUser : actListUsers) {
			System.out.println("expUser: "+expUser);
			System.out.println("actUser: "+actUser);
			if (expUser.equals(actUser)) {
				res = true;
				break;
			}
		}
		Assert.assertEquals(res, result.booleanValue());
	}

	@Then("I expect to see <quantity> users in list")
	@Aliases(values={"I expect to see $quantity users in list"})
	public static void thenIExpectToSeeUsers(@Named("quantity") int quantity) {
		whenISendGetUsersRequest();
		Assert.assertEquals(actListUsers.length,quantity);
	}

}
