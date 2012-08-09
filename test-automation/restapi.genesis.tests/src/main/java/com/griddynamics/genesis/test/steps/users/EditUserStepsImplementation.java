package com.griddynamics.genesis.test.steps.users;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.users.SuccessfulUserCreationResponse;
import com.griddynamics.genesis.tools.users.UserDetails;

public class EditUserStepsImplementation extends UserSharedSources {

	@When("I send request to edit user with userName <userName> and specify email <email> firstName <firstName> lastName <lastName> jobTitle <title> and groups <groups>")
	public void whenISendEditUserRequest(@Named("userName") String userName, @Named("email") String email,
			@Named("firstName") String firstName,
			@Named("lastName") String lastName, @Named("title") String title,
			@Named("groups") String groups) {
			ListUsersStepsImplementation.whenISendGetUsersRequest();
			expUser = new UserDetails(userName, email, firstName, lastName,
					title, "", new String[0]);
			expUser.groups=CommonTools.processStringValue(groups);
			if (doesUserExist(userName)) {				
				request.setUrl(USERS_PATH + "/" + userName);
				request.put(expUser);
			}
	}

	@Then("I expect that user was changed successfully")
	public void thenIExpectSuccessfulUserChanges() {
		SuccessfulUserCreationResponse actResponse = request.getResponseObject(SuccessfulUserCreationResponse.class);
		SuccessfulUserCreationResponse expResponse = new SuccessfulUserCreationResponse(
				true, expUser);
		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
	}
	
	@When("I send request to edit user with name <userName> and specify groups <groups>")
	@Aliases(values={"I send request to edit user with name $userName and specify groups $groups"})
	public void whenISendEditUserShortRequestWithGrups(@Named("userName") String userName,
			@Named("groups") String groups) {
		ListUsersStepsImplementation.whenISendGetUsersRequest();
		UserDetailsStepsImplementation.whenISendViewUserRequest(userName);
		if (doesUserExist(userName)) {	
			expUser = new UserDetails(userName, actUser.email, actUser.firstName, actUser.lastName,
					actUser.jobTitle, "", new String[0]);
			expUser.groups=CommonTools.processStringValue(groups);
			request.setUrl(USERS_PATH + "/" + userName);
			request.put(expUser);
		}
	}

}
