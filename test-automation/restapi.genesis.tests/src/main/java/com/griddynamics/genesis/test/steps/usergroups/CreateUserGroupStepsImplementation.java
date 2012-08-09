package com.griddynamics.genesis.test.steps.usergroups;
import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.testng.Assert;

import com.griddynamics.genesis.tools.CommonTools;
import com.griddynamics.genesis.tools.usergroups.SuccessfulUserGroupCreationResponse;
import com.griddynamics.genesis.tools.usergroups.UserGroupDetails;

public class CreateUserGroupStepsImplementation extends UserGroupSharedSources{

	@When("I send request to create user group with name <groupName> description <description> mailing list <mail> and users <usersList>")
	@Aliases(values={"I send request to create user group with name $groupName description $description mailing list $mail and users $usersList"})
	public void whenISendCreateUserGroupRequest(
			@Named("groupName") String groupName, @Named("description") String descr,
			@Named("mail") String mail, @Named("usersList") String usersList) {
			expGroup=new UserGroupDetails(1, groupName, descr, mail, new String[0]);
			expGroup.users=CommonTools.processStringValue(usersList);
			request.setUrl(GROUPS_PATH);
			request.post(expGroup);
	}
	
	@Then("I expect that user group was created successfully")
	public void thenIExpectSuccessfulGroupCreation(){
		SuccessfulUserGroupCreationResponse actResponse = request.getResponseObject(SuccessfulUserGroupCreationResponse.class);
		SuccessfulUserGroupCreationResponse expResponse=new SuccessfulUserGroupCreationResponse(true, expGroup);
		Assert.assertEquals(actResponse, expResponse);
		Assert.assertEquals(request.checkStatusCode200(), true);
	}
	
	@When("I send request to create user group with name <groupName>")
	public void whenISendCreateUserGroupShortRequest(
			@Named("groupName") String groupName) {
			expGroup=new UserGroupDetails(1, groupName, "description", "", new String[0]);
			request.setUrl(GROUPS_PATH);
			request.post(expGroup);
	}
	
	@When("I send request to create user group with name <groupName> and users <usersList>")
	public void whenISendCreateUserGroupShortRequestWithUsers(
			@Named("groupName") String groupName, @Named("usersList") String usersList) {
			expGroup=new UserGroupDetails(1, groupName, "description", "", new String[0]);
			expGroup.users=CommonTools.processStringValue(usersList);
			request.setUrl(GROUPS_PATH);
			request.post(expGroup);
	}
	
}
